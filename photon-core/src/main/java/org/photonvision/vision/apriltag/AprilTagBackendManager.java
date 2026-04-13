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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.LoadJNI.JNITypes;
import org.photonvision.common.hardware.Platform;

public final class AprilTagBackendManager {
    private static final String BACKEND_PROPERTY = "photonvision.apriltag.backend";
    private static final AtomicReference<String> uiStatus = new AtomicReference<>();
    private static volatile String runtimeFailureReason;
    private static volatile TestOverrides testOverrides;

    private AprilTagBackendManager() {}

    private enum BackendPreference {
        AUTO,
        CPU,
        NVIDIA
    }

    private record TestOverrides(
            Boolean nvidiaCapableLinux, Boolean nvidiaJniLoaded, Boolean nvidiaRuntimeSupported) {}

    public static AprilTagBackendSelection select(AprilTagFamily family) {
        var preference = getBackendPreference();
        if (preference == BackendPreference.CPU) {
            return new AprilTagBackendSelection(
                    AprilTagDetectorBackend.CPU_WPILIB,
                    "AprilTag CPU backend forced by system property",
                    null);
        }

        if (family != AprilTagFamily.kTag36h11) {
            return new AprilTagBackendSelection(
                    AprilTagDetectorBackend.CPU_WPILIB,
                    "AprilTag CPU backend selected because only tag36h11 supports NVIDIA CUDA",
                    preference == BackendPreference.NVIDIA
                            ? "Forced NVIDIA AprilTag backend only supports tag36h11; falling back to CPU."
                            : null);
        }

        if (!isNvidiaCapableLinux()) {
            return new AprilTagBackendSelection(
                    AprilTagDetectorBackend.CPU_WPILIB,
                    "AprilTag CPU backend selected because no NVIDIA-capable Linux device was detected",
                    preference == BackendPreference.NVIDIA
                            ? "Forced NVIDIA AprilTag backend requested, but this host is not an NVIDIA-capable Linux device."
                            : null);
        }

        if (!isNvidiaJniLoaded()) {
            return new AprilTagBackendSelection(
                    AprilTagDetectorBackend.CPU_WPILIB,
                    "AprilTag CPU backend selected because the NVIDIA JNI library is unavailable",
                    preference == BackendPreference.NVIDIA
                            ? "Forced NVIDIA AprilTag backend requested, but the NVIDIA JNI library is unavailable."
                            : null);
        }

        if (!isNvidiaRuntimeSupported()) {
            var reason =
                    runtimeFailureReason != null
                            ? runtimeFailureReason
                            : "the NVIDIA CUDA runtime probe failed";
            return new AprilTagBackendSelection(
                    AprilTagDetectorBackend.CPU_WPILIB,
                    "AprilTag CPU backend selected because " + reason,
                    preference == BackendPreference.NVIDIA
                            ? "Forced NVIDIA AprilTag backend requested, but " + reason + ". Falling back to CPU."
                            : null);
        }

        return new AprilTagBackendSelection(
                AprilTagDetectorBackend.NVIDIA_CUDA,
                preference == BackendPreference.NVIDIA
                        ? "AprilTag NVIDIA CUDA backend forced by system property"
                        : "AprilTag NVIDIA CUDA backend selected automatically",
                null);
    }

    public static void updateActiveBackend(
            AprilTagDetectorBackend backend, String detail, AprilTagFamily family) {
        var status = "AprilTag: " + backend.getDisplayName() + " active";
        if (family != null) {
            status += " (" + family.getNativeName() + ")";
        }
        if (detail != null && !detail.isBlank()) {
            status += " - " + detail;
        }
        uiStatus.set(status);
    }

    public static void markRuntimeFailure(String reason) {
        if (reason == null || reason.isBlank()) {
            runtimeFailureReason = "the NVIDIA CUDA runtime failed";
        } else {
            runtimeFailureReason = reason;
        }
    }

    public static String getUiStatus() {
        var active = uiStatus.get();
        if (active != null && !active.isBlank()) {
            return active;
        }

        if (isNvidiaCapableLinux() && isNvidiaJniLoaded() && isNvidiaRuntimeSupported()) {
            return "AprilTag: NVIDIA CUDA available";
        }

        return "AprilTag: CPU WPILib";
    }

    static void setTestOverrides(
            Boolean nvidiaCapableLinux, Boolean nvidiaJniLoaded, Boolean nvidiaRuntimeSupported) {
        testOverrides = new TestOverrides(nvidiaCapableLinux, nvidiaJniLoaded, nvidiaRuntimeSupported);
    }

    static void resetForTest() {
        testOverrides = null;
        runtimeFailureReason = null;
        uiStatus.set(null);
        System.clearProperty(BACKEND_PROPERTY);
    }

    private static BackendPreference getBackendPreference() {
        var value = System.getProperty(BACKEND_PROPERTY, "auto").trim().toLowerCase(Locale.US);
        return switch (value) {
            case "cpu" -> BackendPreference.CPU;
            case "nvidia" -> BackendPreference.NVIDIA;
            default -> BackendPreference.AUTO;
        };
    }

    private static boolean isNvidiaCapableLinux() {
        if (testOverrides != null && testOverrides.nvidiaCapableLinux() != null) {
            return testOverrides.nvidiaCapableLinux();
        }

        if (!Platform.isLinux()) {
            return false;
        }

        return Files.exists(Path.of("/proc/driver/nvidia/version"))
                || Files.exists(Path.of("/dev/nvidiactl"))
                || fileContains(Path.of("/proc/device-tree/model"), "NVIDIA Jetson");
    }

    private static boolean isNvidiaJniLoaded() {
        if (testOverrides != null && testOverrides.nvidiaJniLoaded() != null) {
            return testOverrides.nvidiaJniLoaded();
        }

        return LoadJNI.hasLoaded(JNITypes.NVIDIA_APRILTAG);
    }

    private static boolean isNvidiaRuntimeSupported() {
        if (testOverrides != null && testOverrides.nvidiaRuntimeSupported() != null) {
            return testOverrides.nvidiaRuntimeSupported();
        }

        if (runtimeFailureReason != null) {
            return false;
        }

        try {
            return org.photonvision.vision.apriltag.NvidiaAprilTagDetector.isRuntimeSupported();
        } catch (Throwable t) {
            runtimeFailureReason = "the NVIDIA CUDA runtime probe threw " + t.getClass().getSimpleName();
            return false;
        }
    }

    private static boolean fileContains(Path path, String text) {
        try {
            if (!Files.exists(path)) {
                return false;
            }

            return Files.readString(path).contains(text);
        } catch (Exception ignored) {
            return false;
        }
    }
}
