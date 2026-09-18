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

package org.photonvision.vision.apriltag;

import java.io.IOException;
import java.util.List;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.LoadJNI.JNITypes;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vkapriltag.VkAprilTagDeviceInfo;
import org.photonvision.vkapriltag.VkAprilTagJNI;

/**
 * Probes, once at startup, whether the Vulkan-accelerated AprilTag detector (vkapriltag_jni) is
 * usable on this coprocessor, and caches the device list for the UI's device picker.
 *
 * <p>{@link #probe()} is deliberately tolerant of every way this can be unavailable: the native
 * library missing entirely (a platform vkapriltag_jni isn't built for, or a dev machine with no
 * native artifacts at all), present but with no Vulkan 1.1+ driver installed, or present with only
 * a software (CPU) Vulkan implementation - {@code VkAprilTagJNI.isSupported()} already excludes
 * that last case, since a silently-selected ~100x-slower software fallback would be
 * indistinguishable from "the GPU port is slow" (see vkapriltag's {@code ContextOptions}). None of
 * these are treated as errors; {@link VkAprilTagPipeline} (once it exists) falls back to the CPU
 * detector in all of them and logs why via {@link #getUnavailableReason}.
 */
public final class VkAprilTagAvailability {
    private static final Logger logger =
            new Logger(VkAprilTagAvailability.class, LogGroup.VisionModule);

    private static volatile boolean supported = false;
    private static volatile List<VkAprilTagDeviceInfo> devices = List.of();

    // Non-null exactly when supported == false, after probe() has run once.
    private static volatile String unavailableReason = "not yet probed";

    private VkAprilTagAvailability() {}

    /**
     * Probes Vulkan availability. Safe to call more than once (e.g. from tests); each call re-runs
     * the probe rather than caching across calls, since {@link #hasProbed} exists for callers that
     * want the cached result instead.
     */
    public static synchronized void probe() {
        try {
            LoadJNI.forceLoad(JNITypes.VKAPRILTAG_DETECTOR);
            supported = VkAprilTagJNI.isSupported();
            devices = supported ? List.of(VkAprilTagJNI.enumerateDevices()) : List.of();
            unavailableReason = supported ? null : "No Vulkan 1.1+ GPU found on this device";
        } catch (UnsatisfiedLinkError | IOException | NoClassDefFoundError e) {
            supported = false;
            devices = List.of();
            unavailableReason = "vkapriltag_jni is not available on this platform";
            logger.debug(
                    "Vulkan AprilTag detector unavailable: "
                            + unavailableReason
                            + " ("
                            + e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage()
                            + ")");
        }

        if (supported) {
            logger.info("Vulkan AprilTag detector available; " + devices.size() + " device(s) found");
        } else {
            logger.info("Vulkan AprilTag detector unavailable: " + unavailableReason);
        }
    }

    public static boolean isSupported() {
        return supported;
    }

    public static List<VkAprilTagDeviceInfo> getDevices() {
        return devices;
    }

    /** Null when {@link #isSupported()} is true. */
    public static String getUnavailableReason() {
        return unavailableReason;
    }
}
