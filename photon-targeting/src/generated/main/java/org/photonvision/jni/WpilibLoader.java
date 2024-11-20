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

package org.photonvision.jni;

import java.io.IOException;

import org.opencv.core.Core;

import edu.wpi.first.apriltag.jni.AprilTagJNI;
import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.cscore.OpenCvLoader;
import edu.wpi.first.hal.JNIWrapper;
import edu.wpi.first.math.jni.ArmFeedforwardJNI;
import edu.wpi.first.math.jni.DAREJNI;
import edu.wpi.first.math.jni.EigenJNI;
import edu.wpi.first.math.jni.Ellipse2dJNI;
import edu.wpi.first.math.jni.Pose3dJNI;
import edu.wpi.first.math.jni.StateSpaceUtilJNI;
import edu.wpi.first.math.jni.TrajectoryUtilJNI;
import edu.wpi.first.net.WPINetJNI;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;

public class WpilibLoader {
    private static boolean has_loaded = false;
    public static boolean loadLibraries() {
        if (has_loaded) return true;

        NetworkTablesJNI.Helper.setExtractOnStaticLoad(false);
        WPIUtilJNI.Helper.setExtractOnStaticLoad(false);
        CameraServerJNI.Helper.setExtractOnStaticLoad(false);
        OpenCvLoader.Helper.setExtractOnStaticLoad(false);
        JNIWrapper.Helper.setExtractOnStaticLoad(false);
        WPINetJNI.Helper.setExtractOnStaticLoad(false);
        AprilTagJNI.Helper.setExtractOnStaticLoad(false);

        // wpimathjni is a bit odd, it's all in the wpimathjni shared lib, but the java side stuff has
        // been split.
        ArmFeedforwardJNI.Helper.setExtractOnStaticLoad(false);
        DAREJNI.Helper.setExtractOnStaticLoad(false);
        EigenJNI.Helper.setExtractOnStaticLoad(false);
        Ellipse2dJNI.Helper.setExtractOnStaticLoad(false);
        Pose3dJNI.Helper.setExtractOnStaticLoad(false);
        StateSpaceUtilJNI.Helper.setExtractOnStaticLoad(false);
        TrajectoryUtilJNI.Helper.setExtractOnStaticLoad(false);

        try {
            CombinedRuntimeLoader.loadLibraries(
                    WpilibLoader.class,
                    "wpiutiljni",
                    "wpimathjni",
                    "ntcorejni",
                    "wpinetjni",
                    "wpiHaljni",
                    "cscorejni",
                    "apriltagjni");

            CombinedRuntimeLoader.loadLibraries(WpilibLoader.class, Core.NATIVE_LIBRARY_NAME);

            has_loaded = true;
        } catch (IOException e) {
            e.printStackTrace();
            has_loaded = false;
        }

        return has_loaded;
    }
}
