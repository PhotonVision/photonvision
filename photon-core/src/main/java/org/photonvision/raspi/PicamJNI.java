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
import org.photonvision.common.hardware.PiVersion;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class PicamJNI {
    private static boolean libraryLoaded = false;
    private static boolean enabled = false; //TODO once we've sorted out what apriltags needs to be doing, we can bring this back?
    private static Logger logger = new Logger(PicamJNI.class, LogGroup.Camera);

    public enum SensorModel {
        Disconnected,
        OV5647, // Picam v1
        IMX219, // Picam v2
        IMX477, // Picam HQ
        Unknown;

        public String getFriendlyName() {
            switch (this) {
                case Disconnected:
                    return "Disconnected Camera";
                case OV5647:
                    return "Camera Module v1";
                case IMX219:
                    return "Camera Module v2";
                case IMX477:
                    return "HQ Camera";
                case Unknown:
                default:
                    return "Unknown Camera";
            }
        }
    }

    public static synchronized void forceLoad() throws IOException {
        if (libraryLoaded || !Platform.isRaspberryPi()) return;

        try {
            File libDirectory = Path.of("lib/").toFile();
            if (!libDirectory.exists()) {
                Files.createDirectory(libDirectory.toPath()).toFile();
            }

            // We always extract the shared object (we could hash each so, but that's a lot of work)
            URL resourceURL = PicamJNI.class.getResource("/nativelibraries/libpicam.so");
            File libFile = Path.of("lib/libpicam.so").toFile();
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

    public static boolean isSupported() {
        return libraryLoaded
                && enabled
                && isVCSMSupported()
                && getSensorModel() != SensorModel.Disconnected
                && Platform.isRaspberryPi()
                && (Platform.currentPiVersion == PiVersion.PI_3
                        || Platform.currentPiVersion == PiVersion.COMPUTE_MODULE_3
                        || Platform.currentPiVersion == PiVersion.ZERO_2_W);
    }

    public static SensorModel getSensorModel() {
        switch (getSensorModelRaw().toLowerCase()) {
            case "":
                return SensorModel.Disconnected;
            case "ov5647":
                return SensorModel.OV5647;
            case "imx219":
                return SensorModel.IMX219;
            case "imx477":
                return SensorModel.IMX477;
            default:
                return SensorModel.Unknown;
        }
    }

    private static native String getSensorModelRaw();

    // This is the main thing we need that isn't supported on Pi 4s, which makes it a good check
    private static native boolean isVCSMSupported();

    // Everything here is static because multiple picams are unsupported at the hardware level

    /**
     * Called once for each video mode change. Starts a native thread running MMAL that stays alive
     * until destroyCamera is called.
     *
     * @return true on error.
     */
    public static native boolean createCamera(int width, int height, int fps);

    /**
     * Destroys MMAL and EGL contexts. Called once for each video mode change *before* createCamera.
     *
     * @return true on error.
     */
    public static native boolean destroyCamera();

    public static native void setThresholds(
            double hL, double sL, double vL, double hU, double sU, double vU);

    public static native void setInvertHue(boolean shouldInvert);

    public static native boolean setExposure(int exposure);

    public static native boolean setBrightness(int brightness);

    // This adjusts the analog gain (normalized to 0-100); ignores the digital gain
    public static native boolean setGain(int gain);

    // Adjusts the auto white balance gains, which are normalized 0-100 in the native code
    public static native boolean setAwbGain(int red, int blue);

    public static native boolean setRotation(int rotation);

    public static native void setShouldCopyColor(boolean shouldCopyColor);

    public static native long getFrameLatency();

    public static native long grabFrame(boolean shouldReturnColor);
}
