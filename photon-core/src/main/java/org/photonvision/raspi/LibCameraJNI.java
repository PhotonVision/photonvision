/*
 * Copyright (C) Photon Vision.
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

package org.photonvision.raspi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class LibCameraJNI {
    private static boolean libraryLoaded = false;
    private static final Logger logger = new Logger(LibCameraJNI.class, LogGroup.Camera);

    public static synchronized void forceLoad() throws IOException {
        if (libraryLoaded) return;

        try {
            File libDirectory = Path.of("lib/").toFile();
            if (!libDirectory.exists()) {
                Files.createDirectory(libDirectory.toPath()).toFile();
            }

            // We always extract the shared object (we could hash each so, but that's a lot of work)
            URL resourceURL = LibCameraJNI.class.getResource("/nativelibraries/libphotonlibcamera.so");
            File libFile = Path.of("lib/libphotonlibcamera.so").toFile();
            try (InputStream in = resourceURL.openStream()) {
                if (libFile.exists()) Files.delete(libFile.toPath());
                Files.copy(in, libFile.toPath());
            } catch (Exception e) {
                logger.error("Could not extract the native library!");
            }
            System.load(libFile.getAbsolutePath());

            libraryLoaded = true;
            logger.info("Successfully loaded libpicam shared object");
        } catch (UnsatisfiedLinkError e) {
            logger.error("Couldn't load libpicam shared object");
            e.printStackTrace();
        }
    }

    public enum SensorModel {
        Disconnected,
        OV5647, // Picam v1
        IMX219, // Picam v2
        IMX708, // Picam v3
        IMX477, // Picam HQ
        OV9281,
        OV7251,
        Unknown;

        public String getFriendlyName() {
            switch (this) {
                case Disconnected:
                    return "Disconnected Camera";
                case OV5647:
                    return "Camera Module v1";
                case IMX219:
                    return "Camera Module v2";
                case IMX708:
                    return "Camera Module v3";
                case IMX477:
                    return "HQ Camera";
                case OV9281:
                    return "OV9281";
                case OV7251:
                    return "OV7251";
                case Unknown:
                default:
                    return "Unknown Camera";
            }
        }
    }

    public static SensorModel getSensorModel(long r_ptr) {
        int model = getSensorModelRaw(r_ptr);
        return SensorModel.values()[model];
    }

    public static SensorModel getSensorModel(String name) {
        int model = getSensorModelRaw(name);
        return SensorModel.values()[model];
    }

    public static boolean isSupported() {
        return libraryLoaded
                // && getSensorModel() != PicamJNI.SensorModel.Disconnected
                // && Platform.isRaspberryPi()
                && isLibraryWorking();
    }

    private static native boolean isLibraryWorking();

    public static native int getSensorModelRaw(long r_ptr);

    public static native int getSensorModelRaw(String name);

    // ======================================================== //

    /**
     * Creates a new runner with a given width/height/fps
     *
     * @param the path / name of the camera as given from libcamera.
     * @param width Camera video mode width in pixels
     * @param height Camera video mode height in pixels
     * @param fps Camera video mode FPS
     * @return the runner pointer for the camera.
     */
    public static native long createCamera(String name, int width, int height, int rotation);

    /**
     * Starts the camera thresholder and display threads running. Make sure that this function is
     * called synchronously with stopCamera and returnFrame!
     */
    public static native boolean startCamera(long r_ptr);

    /** Stops the camera runner. Make sure to call prior to destroying the camera! */
    public static native boolean stopCamera(long r_ptr);

    // Destroy all native resources associated with a camera. Ensure stop is called prior!
    public static native boolean destroyCamera(long r_ptr);

    // ======================================================== //

    // Set thresholds on [0..1]
    public static native boolean setThresholds(
            long r_ptr, double hl, double sl, double vl, double hu, double su, double vu, boolean hueInverted);

    public static native boolean setAutoExposure(long r_ptr, boolean doAutoExposure);

    // Exposure time, in microseconds
    public static native boolean setExposure(long r_ptr, int exposureUs);

    // Set brightness on [-1, 1]
    public static native boolean setBrightness(long r_ptr, double brightness);

    // Unknown ranges for red and blue AWB gain
    public static native boolean setAwbGain(long r_ptr, double red, double blue);

    /**
     * Get the time when the first pixel exposure was started, in the same timebase as libcamera gives
     * the frame capture time. Units are nanoseconds.
     */
    public static native long getFrameCaptureTime(long p_ptr);

    /**
     * Get the current time, in the same timebase as libcamera gives the frame capture time. Units are
     * nanoseconds.
     */
    public static native long getLibcameraTimestamp();

    public static native long setFramesToCopy(long r_ptr, boolean copyIn, boolean copyOut);

    // Analog gain multiplier to apply to all color channels, on [1, Big Number]
    public static native boolean setAnalogGain(long r_ptr, double analog);

    /** Block until a new frame is available from native code. */
    public static native long awaitNewFrame(long r_ptr);

    /**
     * Get a pointer to the most recent color mat generated. Call this immediately after
     * awaitNewFrame, and call only once per new frame!
     */
    public static native long takeColorFrame(long pair_ptr);

    /**
     * Get a pointer to the most recent processed mat generated. Call this immediately after
     * awaitNewFrame, and call only once per new frame!
     */
    public static native long takeProcessedFrame(long pair_ptr);

    /**
     * Set the GPU processing type we should do. Enum of [none, HSV, greyscale, adaptive threshold].
     */
    public static native boolean setGpuProcessType(long r_ptr, int type);

    public static native int getGpuProcessType(long p_ptr);

    /**
     * Release a pair pointer back to the libcamera driver code to be filled again 
     */
    public static native boolean releasePair(long p_ptr);

    /**
     * Get an array containing the names/ids/paths of all connected CSI cameras from libcamera.
     */
    public static native String[] getCameraNames(); 
}
