/*
 * Copyright (C) 2020 Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.pipe.impl;

import static com.jogamp.opengl.GLES2.*;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.egl.EGL;
import com.jogamp.opengl.egl.EGLExt;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import jogamp.opengl.GLOffscreenAutoDrawableImpl;
import jogamp.opengl.egl.EGLContext;
import jogamp.opengl.egl.EGLDrawableFactory;
import org.opencv.core.*;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.raspi.PicamJNI;

public class GPUAccelerator {

    private static final String k_vertexShader =
            String.join(
                    "\n",
                    "#version 100",
                    "",
                    "attribute vec2 position;",
                    "",
                    "void main() {",
                    "  gl_Position = vec4(position, 0.0, 1.0);",
                    "}");
    private static final String k_fragmentShader =
            String.join(
                    "\n",
                    "#version 100",
                    "",
                    "precision lowp float;",
                    "precision lowp int;",
                    "",
                    "uniform vec3 lowerThresh;",
                    "uniform vec3 upperThresh;",
                    "uniform vec2 resolution;",
                    "uniform sampler2D texture0;",
                    "",
                    "vec3 rgb2hsv(vec3 c) {",
                    "  vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);",
                    "  vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));",
                    "  vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));",
                    "",
                    "  float d = q.x - min(q.w, q.y);",
                    "  float e = 1.0e-10;",
                    "  return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);",
                    "}",
                    "",
                    "bool inRange(vec3 hsv) {",
                    "  bvec3 botBool = greaterThanEqual(hsv, lowerThresh);",
                    "  bvec3 topBool = lessThanEqual(hsv, upperThresh);",
                    "  return all(botBool) && all(topBool);",
                    "}",
                    "",
                    "void main() {",
                    "  vec2 uv = gl_FragCoord.xy/resolution;",
                    // Important! We do this .bgr swizzle because the image comes in as BGR but we pretend
                    // it's RGB for convenience+speed
                    "  vec3 col = texture2D(texture0, uv).bgr;", // TODO: Don't use BGR on ZERO_COPY_OMX
                    // Only the first value in the vec4 gets used for GL_RED, and only the last value gets
                    // used for GL_ALPHA
                    "  gl_FragColor = inRange(rgb2hsv(col)) ? vec4(1.0, 1.0, 1.0, 1.0) : vec4(0.0, 0.0, 0.0, 0.0);",
                    //                    "  gl_FragColor = vec4((col.r + col.b + col.g) / 3.0, 1.0, 1.0,
                    // 1.0);",
                    "}");
    private static final float[] k_vertexPositions = {
        // Set up a quad that covers the screen
        -1f, +1f, +1f, +1f, -1f, -1f, +1f, -1f
    };
    private static final int k_positionVertexAttribute =
            0; // ID for the vertex shader position variable
    private static final int EGL_IMAGE_BRCM_VCSM = 0x99930C3;

    public enum TransferMode {
        GL_READ_PIXELS,
        SINGLE_BUFFERED,
        DOUBLE_BUFFERED,
        ZERO_COPY_OMX
    }

    private final IntBuffer vertexVBOIds = GLBuffers.newDirectIntBuffer(1),
            unpackPBOIds = GLBuffers.newDirectIntBuffer(2),
            packPBOIds = GLBuffers.newDirectIntBuffer(2);

    private final GLES2 gl;
    private final GLProfile profile;
    private final int outputFormat;
    private final TransferMode transferMode;
    private final GLOffscreenAutoDrawable drawable;
    private final Texture texture;
    // The texture uniform holds the image that's being processed
    // The resolution uniform holds the current image resolution
    // The lower and upper uniforms hold the lower and upper HSV limits for thresholding
    private final int programId,
            textureUniformId,
            resolutionUniformId,
            lowerUniformId,
            upperUniformId,
            vcsmFbId;
    private final int startingWidth, startingHeight;

    private final Logger logger = new Logger(GPUAccelerator.class, LogGroup.General);

    private byte[] inputBytes, outputBytes;
    private Mat outputMat; // Only used in PBO transfer modes
    private int previousWidth = -1, previousHeight = -1;
    private int unpackIndex = 0, unpackNextIndex = 0, packIndex = 0, packNextIndex = 0;

    public GPUAccelerator(TransferMode transferMode, int startingWidth, int startingHeight) {
        this.transferMode = transferMode;
        this.startingWidth = startingWidth;
        this.startingHeight = startingHeight;

        // Set up GL profile and ask for specific capabilities
        profile =
                GLProfile.get(
                        (transferMode == TransferMode.GL_READ_PIXELS
                                        || transferMode == TransferMode.ZERO_COPY_OMX)
                                ? GLProfile.GLES2
                                : GLProfile.GLES3);
        final var capabilities = new GLCapabilities(profile);
        capabilities.setHardwareAccelerated(true);
        if (transferMode == TransferMode.ZERO_COPY_OMX) {
            // The VideoCore IV closed source driver only works with offscreen PBuffers, not an offscreen
            // FBO
            // The open source driver (Mesa) *does* work with offscreen PBuffers, but OMX doesn't work
            // with Mesa, so we use PBuffers
            capabilities.setPBuffer(true);
        } else {
            capabilities.setFBO(true);
        }
        capabilities.setDoubleBuffered(false);
        capabilities.setOnscreen(false);
        capabilities.setRedBits(8);
        capabilities.setBlueBits(8);
        capabilities.setGreenBits(8);
        capabilities.setAlphaBits(8);

        // Set up the offscreen area we're going to draw to
        final EGLDrawableFactory factory = (EGLDrawableFactory) EGLDrawableFactory.getEGLFactory();
        drawable =
                factory.createOffscreenAutoDrawable(
                        factory.getDefaultDevice(),
                        capabilities,
                        new DefaultGLCapabilitiesChooser(),
                        startingWidth,
                        startingHeight);
        drawable.display();
        drawable.getContext().makeCurrent();

        var sb = new StringBuilder();
        JoglVersion.getDefaultOpenGLInfo(factory.getDefaultDevice(), sb, true);
        logger.trace(sb.toString());

        // Get an OpenGL context; OpenGL ES 2.0 and OpenGL 2.0 are compatible with all the coprocs we
        // care about but not compatible with PBOs. Open GL ES 3.0 and OpenGL 4.0 are compatible with
        // select coprocs *and* PBOs
        gl =
                (transferMode == TransferMode.GL_READ_PIXELS || transferMode == TransferMode.ZERO_COPY_OMX)
                        ? drawable.getGL().getGLES2()
                        : drawable.getGL().getGLES3();
        programId = gl.glCreateProgram();

        if (transferMode == TransferMode.GL_READ_PIXELS
                && !gl.glGetString(GL_EXTENSIONS).contains("GL_EXT_texture_rg")) {
            logger.warn(
                    "OpenGL ES 2.0 implementation does not have the 'GL_EXT_texture_rg' extension, falling back to GL_ALPHA instead of GL_RED output format");
            outputFormat = GL_ALPHA;
        } else {
            outputFormat = GL_RED;
        }

        if (transferMode != TransferMode.ZERO_COPY_OMX) {
            var fboDrawable = (GLOffscreenAutoDrawableImpl.FBOImpl) drawable;

            // JOGL creates a framebuffer color attachment that has RGB set as the format, which is not
            // appropriate for us because we want a single-channel format.
            // We make our own FBO color attachment to remedy this.

            // Detach and destroy the FBO color attachment that JOGL made for us
            fboDrawable.getFBObject(GL_FRONT).detachColorbuffer(gl, 0, true);
            // Equivalent to calling glBindFramebuffer
            fboDrawable.getFBObject(GL_FRONT).bind(gl);
            // Create a color attachment texture to hold our rendered output
            var colorBufferIds = GLBuffers.newDirectIntBuffer(1);
            gl.glGenTextures(1, colorBufferIds);
            gl.glBindTexture(GL_TEXTURE_2D, colorBufferIds.get(0));
            gl.glTexImage2D(
                    GL_TEXTURE_2D,
                    0,
                    outputFormat == GL_RED ? GL_R8 : GL_ALPHA8,
                    startingWidth,
                    startingHeight,
                    0,
                    outputFormat,
                    GL_UNSIGNED_BYTE,
                    null);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            // Attach the texture to the framebuffer
            gl.glBindTexture(GL_TEXTURE_2D, 0);
            gl.glFramebufferTexture2D(
                    GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorBufferIds.get(0), 0);
            // Cleanup
            gl.glBindTexture(GL_TEXTURE_2D, 0);
            fboDrawable.getFBObject(GL_FRONT).unbind(gl);

            this.vcsmFbId = 0; // Unused
        } else {
            // We need to generate a framebuffer and attach an EGLImage set up as a texture to it; the
            // EGLImage must be of the type EGL_IMAGE_BRCM_VCSM so that we can use VCSM to map the images
            // contents without copying

            // First generate our framebuffer and bind it
            var vcsmFbId = GLBuffers.newDirectIntBuffer(1);
            gl.glGenFramebuffers(1, vcsmFbId);
            new DebugGLES2(gl).glBindFramebuffer(GL_FRAMEBUFFER, vcsmFbId.get(0));
            this.vcsmFbId = vcsmFbId.get(0);

            // Then generate the texture that we'll bind our EGLImage to
            var vcsmFbTextureId = GLBuffers.newDirectIntBuffer(1);
            gl.glGenTextures(1, vcsmFbTextureId);
            gl.glBindTexture(GL_TEXTURE_2D, vcsmFbTextureId.get(0));
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            // Create our VCSM info struct
            long vcsmInfo = 0; // PicamJNI.initVCSMInfo(startingWidth, startingHeight);
            if (vcsmInfo == 0) {
                throw new RuntimeException("Couldn't create VCSM info struct (resolution too high?)");
            }

            // Create our EGLImage bound to a pointer to a VCSM info struct
            EGLContext eglContext = (EGLContext) gl.getContext();
            EGLExt eglExt = eglContext.getEGLExt();
            var vcsmFbEglImage =
                    eglExt.eglCreateImageKHR(
                            drawable.getNativeSurface().getDisplayHandle(),
                            EGL.EGL_NO_CONTEXT,
                            EGL_IMAGE_BRCM_VCSM,
                            vcsmInfo,
                            null);
            if (vcsmFbEglImage == EGLExt.EGL_NO_IMAGE) {
                throw new RuntimeException("Failed to create VCSM EGLImage");
            }
            gl.glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, vcsmFbEglImage);

            // Bind our texture to our framebuffer
            gl.glFramebufferTexture2D(
                    GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, vcsmFbTextureId.get(0), 0);
        }

        // Check that the FBO is attached
        int fboStatus = gl.glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (fboStatus == GL_FRAMEBUFFER_UNSUPPORTED) {
            throw new RuntimeException(
                    "GL implementation does not support rendering to internal format '"
                            + String.format("0x%08X", outputFormat == GL_RED ? GL_R8 : GL_ALPHA8)
                            + "' with type '"
                            + String.format("0x%08X", GL_UNSIGNED_BYTE)
                            + "'");
        } else if (fboStatus != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException(
                    "Framebuffer is not complete; framebuffer status is "
                            + String.format("0x%08X", fboStatus));
        }

        logger.info(
                "Created an OpenGL context with renderer '"
                        + gl.glGetString(GL_RENDERER)
                        + "', version '"
                        + gl.glGetString(GL.GL_VERSION)
                        + "', and profile '"
                        + profile.toString()
                        + "'");

        var fmt = GLBuffers.newDirectIntBuffer(1);
        gl.glGetIntegerv(GLES3.GL_IMPLEMENTATION_COLOR_READ_FORMAT, fmt);
        var type = GLBuffers.newDirectIntBuffer(1);
        gl.glGetIntegerv(GLES3.GL_IMPLEMENTATION_COLOR_READ_TYPE, type);

        logger.trace(
                "GL_IMPLEMENTATION_COLOR_READ_FORMAT: "
                        + fmt.get(0)
                        + ", GL_IMPLEMENTATION_COLOR_READ_TYPE: "
                        + type.get(0));

        // Tell OpenGL that the attribute in the vertex shader named position is bound to index 0 (the
        // index for the generic position input)
        gl.glBindAttribLocation(programId, k_positionVertexAttribute, "position");

        // Compile and setup our two shaders with our program
        final int vertexId = createShader(gl, programId, k_vertexShader, GL_VERTEX_SHADER);
        final int fragmentId = createShader(gl, programId, k_fragmentShader, GL_FRAGMENT_SHADER);

        // Link our program together and check for errors
        gl.glLinkProgram(programId);
        IntBuffer status = GLBuffers.newDirectIntBuffer(1);
        gl.glGetProgramiv(programId, GL_LINK_STATUS, status);
        if (status.get(0) == GL_FALSE) {

            IntBuffer infoLogLength = GLBuffers.newDirectIntBuffer(1);
            gl.glGetProgramiv(programId, GL_INFO_LOG_LENGTH, infoLogLength);

            ByteBuffer bufferInfoLog = GLBuffers.newDirectByteBuffer(infoLogLength.get(0));
            gl.glGetProgramInfoLog(programId, infoLogLength.get(0), null, bufferInfoLog);
            byte[] bytes = new byte[infoLogLength.get(0)];
            bufferInfoLog.get(bytes);
            String strInfoLog = new String(bytes);

            throw new RuntimeException("Linker failure: " + strInfoLog);
        }
        gl.glValidateProgram(programId);

        // Cleanup shaders that are now compiled in
        gl.glDetachShader(programId, vertexId);
        gl.glDetachShader(programId, fragmentId);
        gl.glDeleteShader(vertexId);
        gl.glDeleteShader(fragmentId);

        // Tell OpenGL to use our program
        gl.glUseProgram(programId);

        // Initialize projection environment
        gl.glViewport(0, 0, startingWidth, startingHeight);

        // Set up our texture
        texture = new Texture(GL_TEXTURE_2D);
        texture.bind(gl);
        if (transferMode == TransferMode.ZERO_COPY_OMX) {
            gl.glTexImage2D(
                    GL_TEXTURE_2D,
                    0,
                    GL_RGB,
                    startingWidth,
                    startingHeight,
                    0,
                    GL_RGB,
                    GL_UNSIGNED_BYTE,
                    null);
        }
        texture.setTexParameteri(gl, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        texture.setTexParameteri(gl, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        texture.setTexParameteri(gl, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        texture.setTexParameteri(gl, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        if (transferMode == TransferMode.ZERO_COPY_OMX) {
            // First we get pointers to our current EGLDisplay, EGLSurface, and EGLContext
            // We don't have to call any of the EGL init functions, all the EGL devices are premade

            // The EGLContext provides a gateway to what we want
            EGLContext eglContext = (EGLContext) gl.getContext();
            EGLExt eglExt = eglContext.getEGLExt();

            // Next we use our EGLExt to map our image texture to EGL
            var params = Buffers.newDirectIntBuffer(1);
            params.put(0, EGL.EGL_NONE);
            long eglImageHandle =
                    eglExt.eglCreateImageKHR(
                            drawable.getNativeSurface().getDisplayHandle(),
                            eglContext.getHandle(),
                            EGLExt.EGL_GL_TEXTURE_2D_KHR,
                            texture.getTextureObject(),
                            params);
            // Is this the same as EGL_NO_IMAGE_KHR? Seems to be ok and notices if we have an error.
            if (eglImageHandle == EGLExt.EGL_NO_IMAGE) {
                throw new RuntimeException("EGLImage handle is invalid");
            }

            // Finally we pass the EGLImage handle to the native code for use by OMX
            logger.info("Setting EGLImage handle");
            boolean err = true; // PicamJNI.setEGLImageHandle(eglImageHandle);
            if (err) {
                throw new RuntimeException("Couldn't set EGLImage handle");
            }

            // Start the OMX capture thread in native code
            err = PicamJNI.createCamera(startingWidth, startingHeight, 90);
            if (err) {
                throw new RuntimeException("Couldn't create native OMX capture thread");
            }
        }

        // Set up a uniform holding our image as a texture
        textureUniformId = gl.glGetUniformLocation(programId, "texture0");
        gl.glUniform1i(textureUniformId, 0);

        // Set up a uniform to hold image resolution
        resolutionUniformId = gl.glGetUniformLocation(programId, "resolution");

        // Set up uniforms for the HSV thresholds
        lowerUniformId = gl.glGetUniformLocation(programId, "lowerThresh");
        upperUniformId = gl.glGetUniformLocation(programId, "upperThresh");

        // Clear the whole screen (front buffer)
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Set up a quad that covers the entire screen so that our fragment shader draws onto the entire
        // screen
        gl.glGenBuffers(1, vertexVBOIds);

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(k_vertexPositions);
        gl.glBindBuffer(GL_ARRAY_BUFFER, vertexVBOIds.get(0));
        gl.glBufferData(
                GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);

        if (transferMode == TransferMode.SINGLE_BUFFERED
                || transferMode == TransferMode.DOUBLE_BUFFERED) {
            outputMat = new Mat(startingHeight, startingWidth, CvType.CV_8UC1);

            // Set up pixel unpack buffer (a PBO to transfer image data to the GPU)
            gl.glGenBuffers(2, unpackPBOIds);

            gl.glBindBuffer(GLES3.GL_PIXEL_UNPACK_BUFFER, unpackPBOIds.get(0));
            gl.glBufferData(
                    GLES3.GL_PIXEL_UNPACK_BUFFER,
                    startingHeight * startingWidth * 3,
                    null,
                    GLES3.GL_STREAM_DRAW);
            if (transferMode == TransferMode.DOUBLE_BUFFERED) {
                gl.glBindBuffer(GLES3.GL_PIXEL_UNPACK_BUFFER, unpackPBOIds.get(1));
                gl.glBufferData(
                        GLES3.GL_PIXEL_UNPACK_BUFFER,
                        startingHeight * startingWidth * 3,
                        null,
                        GLES3.GL_STREAM_DRAW);
            }
            gl.glBindBuffer(GLES3.GL_PIXEL_UNPACK_BUFFER, 0);

            // Set up pixel pack buffer (a PBO to transfer the processed image back from the GPU)
            gl.glGenBuffers(2, packPBOIds);

            gl.glBindBuffer(GLES3.GL_PIXEL_PACK_BUFFER, packPBOIds.get(0));
            gl.glBufferData(
                    GLES3.GL_PIXEL_PACK_BUFFER, startingHeight * startingWidth, null, GLES3.GL_STREAM_READ);
            if (transferMode == TransferMode.DOUBLE_BUFFERED) {
                gl.glBindBuffer(GLES3.GL_PIXEL_PACK_BUFFER, packPBOIds.get(1));
                gl.glBufferData(
                        GLES3.GL_PIXEL_PACK_BUFFER, startingHeight * startingWidth, null, GLES3.GL_STREAM_READ);
            }
            gl.glBindBuffer(GLES3.GL_PIXEL_PACK_BUFFER, 0);
        }
    }

    private static int createShader(GLES2 gl, int programId, String glslCode, int shaderType) {
        int shaderId = gl.glCreateShader(shaderType);
        if (shaderId == 0) throw new RuntimeException("Shader ID is zero");

        IntBuffer length = GLBuffers.newDirectIntBuffer(new int[] {glslCode.length()});
        gl.glShaderSource(shaderId, 1, new String[] {glslCode}, length);
        gl.glCompileShader(shaderId);

        IntBuffer intBuffer = IntBuffer.allocate(1);
        gl.glGetShaderiv(shaderId, GL_COMPILE_STATUS, intBuffer);

        if (intBuffer.get(0) != 1) {
            gl.glGetShaderiv(shaderId, GL_INFO_LOG_LENGTH, intBuffer);
            int size = intBuffer.get(0);
            if (size > 0) {
                ByteBuffer byteBuffer = ByteBuffer.allocate(size);
                gl.glGetShaderInfoLog(shaderId, size, intBuffer, byteBuffer);
                System.err.println(new String(byteBuffer.array()));
            }
            throw new RuntimeException("Couldn't compile shader");
        }

        gl.glAttachShader(programId, shaderId);

        return shaderId;
    }

    boolean hasCalled = false;

    public void redrawGL(Scalar hsvLower, Scalar hsvUpper) {
        // PicamJNI.waitForOMXFillBufferDone();

        // Bind the framebuffer we'll draw to
        gl.glBindFramebuffer(GL_FRAMEBUFFER, vcsmFbId);

        // Set the viewport to the correct size and clear the screen
        gl.glViewport(0, 0, startingWidth, startingHeight);
        gl.glClear(GL_COLOR_BUFFER_BIT);

        // Use the shaders we set up in the constructor
        gl.glUseProgram(programId);

        // Load and bind our image as a 2D texture
        gl.glActiveTexture(GL_TEXTURE0);
        texture.enable(gl);
        texture.bind(gl);

        // Reset the fullscreen quad
        gl.glBindBuffer(GL_ARRAY_BUFFER, vertexVBOIds.get(0));
        gl.glVertexAttribPointer(k_positionVertexAttribute, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(k_positionVertexAttribute);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        // Texture is presumed to contain valid camera data at this point because it points to memory
        // managed by OpenMAX

        // Put values in a uniform holding the image resolution
        gl.glUniform2f(resolutionUniformId, startingWidth, startingHeight);

        // Put values in threshold uniforms
        var lowr = hsvLower.val;
        var upr = hsvUpper.val;
        gl.glUniform3f(
                lowerUniformId, (float) lowr[0] / 255, (float) lowr[1] / 255, (float) lowr[2] / 255);
        gl.glUniform3f(
                upperUniformId, (float) upr[0] / 255, (float) upr[1] / 255, (float) upr[2] / 255);

        // Draw the fullscreen quad
        gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        gl.glFlush();
        gl.glFinish();

        // Cleanup
        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        texture.disable(gl);
        gl.glDisableVertexAttribArray(k_positionVertexAttribute);
        gl.glUseProgram(0);
    }

    protected Mat process(Mat in, Scalar hsvLower, Scalar hsvUpper) {
        if (transferMode == TransferMode.ZERO_COPY_OMX)
            throw new RuntimeException(
                    "This method does not support DIRECT_OMX mode; use redrawGL instead along with PicamJNI::grabFrame");

        if (in.width() != previousWidth && in.height() != previousHeight) {
            logger.debug("Resizing OpenGL viewport, byte buffers, and PBOs");

            drawable.setSurfaceSize(in.width(), in.height());
            gl.glViewport(0, 0, in.width(), in.height());

            previousWidth = in.width();
            previousHeight = in.height();

            inputBytes = new byte[in.width() * in.height() * 3];

            outputBytes = new byte[in.width() * in.height()];
            outputMat = new Mat(startingHeight, startingWidth, CvType.CV_8UC1);

            if (transferMode != TransferMode.GL_READ_PIXELS) {
                gl.glBindBuffer(GLES3.GL_PIXEL_PACK_BUFFER, packPBOIds.get(0));
                gl.glBufferData(
                        GLES3.GL_PIXEL_PACK_BUFFER, in.width() * in.height(), null, GLES3.GL_STREAM_READ);

                if (transferMode == TransferMode.DOUBLE_BUFFERED) {
                    gl.glBindBuffer(GLES3.GL_PIXEL_PACK_BUFFER, packPBOIds.get(1));
                    gl.glBufferData(
                            GLES3.GL_PIXEL_PACK_BUFFER, in.width() * in.height(), null, GLES3.GL_STREAM_READ);
                }
            }
        }

        if (transferMode == TransferMode.DOUBLE_BUFFERED) {
            unpackIndex = (unpackIndex + 1) % 2;
            unpackNextIndex = (unpackIndex + 1) % 2;
        }

        // Use the shaders we set up in the constructor
        gl.glUseProgram(programId);

        // Reset the fullscreen quad
        gl.glBindBuffer(GL_ARRAY_BUFFER, vertexVBOIds.get(0));
        gl.glEnableVertexAttribArray(k_positionVertexAttribute);
        gl.glVertexAttribPointer(k_positionVertexAttribute, 2, GL_FLOAT, false, 0, 0);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        // Load and bind our image as a 2D texture
        gl.glActiveTexture(GL_TEXTURE0);
        texture.enable(gl);
        texture.bind(gl);

        // Load our image into the texture
        in.get(0, 0, inputBytes);
        if (transferMode == TransferMode.GL_READ_PIXELS) {
            ByteBuffer buf = ByteBuffer.wrap(inputBytes);
            // (We're actually taking in BGR even though this says RGB; it's much easier and faster to
            // switch it around in the fragment shader)
            texture.updateImage(
                    gl,
                    new TextureData(
                            profile,
                            GL_RGB,
                            in.width(),
                            in.height(),
                            0,
                            GL_RGB,
                            GL_UNSIGNED_BYTE,
                            false,
                            false,
                            false,
                            buf,
                            null));
        } else {
            // Bind the PBO to the texture
            gl.glBindBuffer(GLES3.GL_PIXEL_UNPACK_BUFFER, unpackPBOIds.get(unpackIndex));

            // Copy pixels from the PBO to the texture object
            gl.glTexSubImage2D(
                    GLES3.GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    in.width(),
                    in.height(),
                    GLES3.GL_RGB8,
                    GLES3.GL_UNSIGNED_BYTE,
                    0);

            // Bind (potentially) another PBO to update the texture source
            gl.glBindBuffer(GLES3.GL_PIXEL_UNPACK_BUFFER, unpackPBOIds.get(unpackNextIndex));

            // This call with a nullptr for the data arg tells OpenGL *not* to wait to be in sync with the
            // GPU
            // This causes the previous data in the PBO to be discarded
            gl.glBufferData(
                    GLES3.GL_PIXEL_UNPACK_BUFFER, in.width() * in.height() * 3, null, GLES3.GL_STREAM_DRAW);

            // Map the a buffer of GPU memory into a place that's accessible by us
            var buf = gl.glMapBuffer(GLES3.GL_PIXEL_UNPACK_BUFFER, GLES3.GL_WRITE_ONLY);
            buf.put(inputBytes);

            gl.glUnmapBuffer(GLES3.GL_PIXEL_UNPACK_BUFFER);
            gl.glBindBuffer(GLES3.GL_PIXEL_UNPACK_BUFFER, 0);
        }

        // Put values in a uniform holding the image resolution
        gl.glUniform2f(resolutionUniformId, in.width(), in.height());

        // Put values in threshold uniforms
        var lowr = hsvLower.val;
        var upr = hsvUpper.val;
        gl.glUniform3f(lowerUniformId, (float) lowr[0], (float) lowr[1], (float) lowr[2]);
        gl.glUniform3f(upperUniformId, (float) upr[0], (float) upr[1], (float) upr[2]);

        // Draw the fullscreen quad
        gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        // Cleanup
        texture.disable(gl);
        gl.glDisableVertexAttribArray(k_positionVertexAttribute);
        gl.glUseProgram(0);

        if (transferMode == TransferMode.GL_READ_PIXELS) {
            return saveMatNoPBO(gl, in.width(), in.height());
        } else {
            return saveMatPBO(
                    (GLES3) gl, in.width(), in.height(), transferMode == TransferMode.DOUBLE_BUFFERED);
        }
    }

    private Mat saveMatNoPBO(GLES2 gl, int width, int height) {
        ByteBuffer buffer = GLBuffers.newDirectByteBuffer(width * height * 4);
        // We use GL_RED/GL_ALPHA to get things in a single-channel format
        // Note that which pixel format you use is *very* important for performance
        // E.g. GL_ALPHA is super slow in this case
        gl.glReadPixels(0, 0, width, height, outputFormat, GL_UNSIGNED_BYTE, buffer);

        return new Mat(height, width, CvType.CV_8UC4, buffer);
    }

    private Mat saveMatPBO(GLES3 gl, int width, int height, boolean doubleBuffered) {
        if (doubleBuffered) {
            packIndex = (packIndex + 1) % 2;
            packNextIndex = (packIndex + 1) % 2;
        }

        // Set the target framebuffer attachment to read
        gl.glReadBuffer(GLES3.GL_COLOR_ATTACHMENT0);

        // Read pixels from the framebuffer to the PBO
        gl.glBindBuffer(GLES3.GL_PIXEL_PACK_BUFFER, packPBOIds.get(packIndex));
        // We use GL_RED (which is always supported in GLES3) to get things in a single-channel format
        // Note that which pixel format you use is *very* important to performance
        // E.g. GL_ALPHA is super slow in this case
        gl.glReadPixels(0, 0, width, height, GLES3.GL_RED, GLES3.GL_UNSIGNED_BYTE, 0);

        // Map the PBO into the CPU's memory
        gl.glBindBuffer(GLES3.GL_PIXEL_PACK_BUFFER, packPBOIds.get(packNextIndex));
        var buf =
                gl.glMapBufferRange(GLES3.GL_PIXEL_PACK_BUFFER, 0, width * height, GLES3.GL_MAP_READ_BIT);
        buf.get(outputBytes);
        outputMat.put(0, 0, outputBytes);
        gl.glUnmapBuffer(GLES3.GL_PIXEL_PACK_BUFFER);
        gl.glBindBuffer(GLES3.GL_PIXEL_PACK_BUFFER, 0);

        return outputMat;
    }
}
