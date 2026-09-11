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

package org.photonvision.vision.pipe.impl;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.util.ArrayList;
import java.util.List;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.apriltag.VkAprilTagAvailability;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vkapriltag.VkAprilTagJNI;
import org.wpilib.vision.apriltag.AprilTagDetection;
import org.wpilib.vision.apriltag.AprilTagDetector;

/**
 * BETA. Wraps the Vulkan-accelerated (vkapriltag_jni) detector, falling back to WPILib's CPU
 * AprilTagDetector - transparently, with no change in output shape - whenever the Vulkan backend
 * can't run for this frame's configuration. See {@link #createOrFallBack} for the specific
 * conditions that trigger a fallback.
 *
 * <p>Per-tag results cross the JNI boundary as one flat {@code double[]}: a leading count, then 22
 * doubles per tag (id, hamming, decision margin, center x/y, 4 corners as x0,y0,...,x3,y3, then the
 * row-major 3x3 homography) - see {@code VkAprilTagJNI.detect}'s Javadoc in vkapriltag-jni.
 */
public class VkAprilTagDetectionPipe
        extends CVPipe<
                CVMat, List<AprilTagDetection>, VkAprilTagDetectionPipe.VkAprilTagDetectionPipeParams> {
    private static final Logger logger =
            new Logger(VkAprilTagDetectionPipe.class, LogGroup.VisionModule);

    private static final Cleaner cleaner = Cleaner.create();
    private static final int DOUBLES_PER_TAG = 22;

    private Cleanable cleanable;
    private long nativeHandle = 0;

    // Non-null exactly when nativeHandle == 0 - i.e. exactly when we're on the fallback path.
    private AprilTagDetector cpuFallbackDetector;

    @Override
    public void setParams(VkAprilTagDetectionPipeParams newParams) {
        // Every field of VkAprilTagDetectionPipeParams (family, width, height, decimation,
        // deviceIndex, cpuThreads) feeds detector construction - vkapriltag supports one
        // family/resolution/decimation/device per detector instance (see DetectorHandle in
        // vkapriltag-jni), so any change here means destroying and recreating it, exactly as
        // AprilTagDetectionPipe's own !equals() guard does for the CPU detector.
        if (this.params == null || !this.params.equals(newParams)) {
            releaseDetector();
            createOrFallBack(newParams);
        }
        super.setParams(newParams);
    }

    /**
     * Tries to build the Vulkan detector; falls back to a CPU AprilTagDetector, logging why, on any
     * of: Vulkan unsupported on this platform, a frame size that isn't evenly divisible by the
     * configured decimation factor (vkapriltag's requirement as of v1.3.0 - decimation is now
     * configurable, replacing the old fixed "multiple of 8" rule), or a native create() failure.
     */
    private void createOrFallBack(VkAprilTagDetectionPipeParams p) {
        if (!VkAprilTagAvailability.isSupported()) {
            fallBackToCpu(p, "Vulkan unavailable: " + VkAprilTagAvailability.getUnavailableReason());
            return;
        }
        // VkAprilTagJNI.validateGeometry() is the single source of truth for this check (it also
        // covers the packed-size ceiling, which is unreachable at any realistic camera resolution
        // and so isn't worth hand-duplicating here) - this pre-check just avoids paying for a
        // doomed native create() call (and the Vulkan Context/pipeline construction cost it would
        // pay before failing) on the common case of a decimation that doesn't fit the current
        // camera mode.
        String geometryError = VkAprilTagJNI.validateGeometry(p.width(), p.height(), p.decimation());
        if (geometryError != null) {
            fallBackToCpu(p, geometryError);
            return;
        }

        long handle =
                VkAprilTagJNI.create(
                        p.width(),
                        p.height(),
                        p.decimation(),
                        p.family().getNativeName(),
                        p.cpuThreads(),
                        p.deviceIndex());
        if (handle == 0) {
            fallBackToCpu(p, "native create() failed: " + VkAprilTagJNI.getLastError());
            return;
        }

        nativeHandle = handle;
        cpuFallbackDetector = null;
        final long handleToClean = handle;
        cleanable = cleaner.register(this, () -> VkAprilTagJNI.destroy(handleToClean));
        logger.info("Vulkan AprilTag detector created (" + p.family() + ", " + p.width() + "x" + p.height() + ")");
    }

    private void fallBackToCpu(VkAprilTagDetectionPipeParams p, String reason) {
        logger.warn("Falling back to CPU AprilTag detection: " + reason);
        nativeHandle = 0;
        cpuFallbackDetector = new AprilTagDetector();
        cpuFallbackDetector.addFamily(p.family().getNativeName());
    }

    /** True while this frame's detections actually came off the GPU, not the CPU fallback. */
    public boolean isVulkanActive() {
        return nativeHandle != 0;
    }

    @Override
    protected List<AprilTagDetection> process(CVMat in) {
        if (in.getMat().empty()) {
            return List.of();
        }

        if (cpuFallbackDetector != null) {
            var ret = cpuFallbackDetector.detect(in.getMat());
            return ret == null ? List.of() : List.of(ret);
        }

        if (nativeHandle == 0) {
            throw new IllegalStateException("VkAprilTagDetectionPipe used before setParams()");
        }

        double[] flat = VkAprilTagJNI.detect(nativeHandle, in.getMat().getNativeObjAddr());
        if (flat == null) {
            logger.error("Vulkan detect() failed: " + VkAprilTagJNI.getLastError());
            return List.of();
        }

        int n = (int) flat[0];
        List<AprilTagDetection> detections = new ArrayList<>(n);
        String familyName = params.family().getNativeName();
        for (int i = 0; i < n; i++) {
            int base = 1 + i * DOUBLES_PER_TAG;
            int id = (int) flat[base];
            int hamming = (int) flat[base + 1];
            float decisionMargin = (float) flat[base + 2];
            double centerX = flat[base + 3];
            double centerY = flat[base + 4];
            double[] corners = new double[8];
            System.arraycopy(flat, base + 5, corners, 0, 8);
            double[] homography = new double[9];
            System.arraycopy(flat, base + 13, homography, 0, 9);

            detections.add(
                    new AprilTagDetection(
                            familyName, id, hamming, decisionMargin, homography, centerX, centerY, corners));
        }
        return detections;
    }

    private void releaseDetector() {
        if (cleanable != null) {
            cleanable.clean();
            cleanable = null;
        }
        nativeHandle = 0;
        cpuFallbackDetector = null;
    }

    @Override
    public void release() {
        releaseDetector();
    }

    public record VkAprilTagDetectionPipeParams(
            AprilTagFamily family,
            int width,
            int height,
            int decimation,
            int deviceIndex,
            int cpuThreads) {}
}
