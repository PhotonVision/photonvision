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

package org.photonvision.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.apriltag.jni.AprilTagJNI;
import edu.wpi.first.cscore.CameraServerCvJNI;
import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.hal.JNIWrapper;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.net.WPINetJNI;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.RuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;

public class TestUtils {
    public static boolean loadLibraries() {
        JNIWrapper.Helper.setExtractOnStaticLoad(false);
        WPIUtilJNI.Helper.setExtractOnStaticLoad(false);
        NetworkTablesJNI.Helper.setExtractOnStaticLoad(false);
        WPINetJNI.Helper.setExtractOnStaticLoad(false);
        CameraServerJNI.Helper.setExtractOnStaticLoad(false);
        CameraServerCvJNI.Helper.setExtractOnStaticLoad(false);
        AprilTagJNI.Helper.setExtractOnStaticLoad(false);

        try {
            var loader =
                    new RuntimeLoader<>(
                            Core.NATIVE_LIBRARY_NAME, RuntimeLoader.getDefaultExtractionRoot(), Core.class);
            loader.loadLibrary();

            CombinedRuntimeLoader.loadLibraries(
                    TestUtils.class,
                    "wpiutiljni",
                    "ntcorejni",
                    "wpinetjni",
                    "wpiHaljni",
                    "cscorejni",
                    "cscorejnicvstatic",
                    "apriltagjni");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unused")
    public enum WPI2019Image {
        kCargoAngledDark48in(1.2192),
        kCargoSideStraightDark36in(0.9144),
        kCargoSideStraightDark60in(1.524),
        kCargoSideStraightDark72in(1.8288),
        kCargoSideStraightPanelDark36in(0.9144),
        kCargoStraightDark19in(0.4826),
        kCargoStraightDark24in(0.6096),
        kCargoStraightDark48in(1.2192),
        kCargoStraightDark72in(1.8288),
        kCargoStraightDark72in_HighRes(1.8288),
        kCargoStraightDark90in(2.286),
        kRocketPanelAngleDark48in(1.2192),
        kRocketPanelAngleDark60in(1.524);

        public static double FOV = 68.5;

        public final double distanceMeters;
        public final Path path;

        Path getPath() {
            var filename = this.toString().substring(1);
            return Path.of("2019", "WPI", filename + ".jpg");
        }

        WPI2019Image(double distanceMeters) {
            this.distanceMeters = distanceMeters;
            this.path = getPath();
        }
    }

    @SuppressWarnings("unused")
    public enum WPI2020Image {
        kBlueGoal_060in_Center(1.524),
        kBlueGoal_084in_Center(2.1336),
        kBlueGoal_084in_Center_720p(2.1336),
        kBlueGoal_108in_Center(2.7432),
        kBlueGoal_132in_Center(3.3528),
        kBlueGoal_156in_Center(3.9624),
        kBlueGoal_180in_Center(4.572),
        kBlueGoal_156in_Left(3.9624),
        kBlueGoal_224in_Left(5.6896),
        kBlueGoal_228in_ProtectedZone(5.7912),
        kBlueGoal_330in_ProtectedZone(8.382),
        kBlueGoal_Far_ProtectedZone(10.668), // TODO: find a more accurate distance
        kRedLoading_016in_Down(0.4064),
        kRedLoading_030in_Down(0.762),
        kRedLoading_048in_Down(1.2192),
        kRedLoading_048in(1.2192),
        kRedLoading_060in(1.524),
        kRedLoading_084in(2.1336),
        kRedLoading_108in(2.7432);

        public static double FOV = 68.5;

        public final double distanceMeters;
        public final Path path;

        Path getPath() {
            var filename = this.toString().substring(1).replace('_', '-');
            return Path.of("2020", "WPI", filename + ".jpg");
        }

        WPI2020Image(double distanceMeters) {
            this.distanceMeters = distanceMeters;
            this.path = getPath();
        }
    }

    public enum WPI2023Apriltags {
        k162_36_Angle,
        k162_36_Straight,
        k383_60_Angle2;

        public static double FOV = 68.5;

        public final Translation2d approxPose;
        public final Path path;

        Path getPath() {
            var filename = this.toString().substring(1);
            return Path.of("2023", "AprilTags", filename + ".png");
        }

        Translation2d getPose() {
            var names = this.toString().substring(1).split("_");
            var x = Units.inchesToMeters(Integer.parseInt(names[0]));
            var y = Units.inchesToMeters(Integer.parseInt(names[1]));
            return new Translation2d(x, y);
        }

        WPI2023Apriltags() {
            this.approxPose = getPose();
            this.path = getPath();
        }
    }

    public enum WPI2022Image {
        kTerminal12ft6in(Units.feetToMeters(12.5)),
        kTerminal22ft6in(Units.feetToMeters(22.5));

        public static double FOV = 68.5;

        public final double distanceMeters;
        public final Path path;

        Path getPath() {
            var filename = this.toString().substring(1).replace('_', '-');
            return Path.of("2022", "WPI", filename + ".png");
        }

        WPI2022Image(double distanceMeters) {
            this.distanceMeters = distanceMeters;
            this.path = getPath();
        }
    }

    public enum PolygonTestImages {
        kPolygons;

        public final Path path;

        Path getPath() {
            var filename = this.toString().substring(1).toLowerCase();
            return Path.of("polygons", filename + ".png");
        }

        PolygonTestImages() {
            this.path = getPath();
        }
    }

    public enum PowercellTestImages {
        kPowercell_test_1,
        kPowercell_test_2,
        kPowercell_test_3,
        kPowercell_test_4,
        kPowercell_test_5,
        kPowercell_test_6;

        public final Path path;

        Path getPath() {
            var filename = this.toString().substring(1).toLowerCase();
            return Path.of(filename + ".png");
        }

        PowercellTestImages() {
            this.path = getPath();
        }
    }

    public enum ApriltagTestImages {
        kRobots,
        kTag1_640_480,
        kTag1_16h5_1280,
        kTag_corner_1280;

        public final Path path;

        Path getPath() {
            // Strip leading k
            var filename = this.toString().substring(1).toLowerCase();
            var extension = ".jpg";
            if (filename.equals("tag1_16h5_1280")) extension = ".png";
            return Path.of("apriltag", filename + extension);
        }

        ApriltagTestImages() {
            this.path = getPath();
        }
    }

    public static Path getResourcesFolderPath(boolean testMode) {
        System.out.println("CWD: " + Path.of("").toAbsolutePath().toString());

        // VSCode likes to make this path relative to the wrong root directory, so a fun hack to tell
        // if it's wrong
        Path ret = Path.of("test-resources").toAbsolutePath();
        if (Path.of("test-resources")
                .toAbsolutePath()
                .toString()
                .replace("/", "")
                .replace("\\", "")
                .toLowerCase()
                .matches(".*photon-[a-z]*test-resources")) {
            ret = Path.of("../test-resources").toAbsolutePath();
        }
        return ret;
    }

    public static Path getTestMode2019ImagePath() {
        return getResourcesFolderPath(true)
                .resolve("testimages")
                .resolve(WPI2019Image.kRocketPanelAngleDark60in.path);
    }

    public static Path getTestMode2020ImagePath() {
        return getResourcesFolderPath(true)
                .resolve("testimages")
                .resolve(WPI2020Image.kBlueGoal_156in_Left.path);
    }

    public static Path getTestMode2022ImagePath() {
        return getResourcesFolderPath(true)
                .resolve("testimages")
                .resolve(WPI2022Image.kTerminal22ft6in.path);
    }

    public static Path getTestModeApriltagPath() {
        return getResourcesFolderPath(true)
                .resolve("testimages")
                .resolve(ApriltagTestImages.kRobots.path);
    }

    public static Path getTestImagesPath(boolean testMode) {
        return getResourcesFolderPath(testMode).resolve("testimages");
    }

    public static Path getCalibrationPath(boolean testMode) {
        return getResourcesFolderPath(testMode).resolve("calibration");
    }

    public static Path getPowercellPath(boolean testMode) {
        return getTestImagesPath(testMode).resolve("polygons").resolve("powercells");
    }

    public static Path getWPIImagePath(WPI2020Image image, boolean testMode) {
        return getTestImagesPath(testMode).resolve(image.path);
    }

    public static Path getWPIImagePath(WPI2019Image image, boolean testMode) {
        return getTestImagesPath(testMode).resolve(image.path);
    }

    public static Path getPolygonImagePath(PolygonTestImages image, boolean testMode) {
        return getTestImagesPath(testMode).resolve(image.path);
    }

    public static Path getApriltagImagePath(ApriltagTestImages image, boolean testMode) {
        return getTestImagesPath(testMode).resolve(image.path);
    }

    public static Path getPowercellImagePath(PowercellTestImages image, boolean testMode) {
        return getPowercellPath(testMode).resolve(image.path);
    }

    public static Path getDotBoardImagesPath() {
        return getResourcesFolderPath(false).resolve("calibrationBoardImages");
    }

    public static Path getSquaresBoardImagesPath() {
        return getResourcesFolderPath(false).resolve("calibrationSquaresImg");
    }

    public static File getHardwareConfigJson() {
        return getResourcesFolderPath(false)
                .resolve("hardware")
                .resolve("HardwareConfig.json")
                .toFile();
    }

    private static final String LIFECAM_240P_CAL_FILE = "lifecam240p.json";
    private static final String LIFECAM_480P_CAL_FILE = "lifecam480p.json";
    public static final String LIFECAM_1280P_CAL_FILE = "lifecam_1280.json";
    public static final String LIMELIGHT_480P_CAL_FILE = "limelight_1280_720.json";

    public static CameraCalibrationCoefficients getCoeffs(String filename, boolean testMode) {
        try {
            return new ObjectMapper()
                    .readValue(
                            (Path.of(getCalibrationPath(testMode).toString(), filename).toFile()),
                            CameraCalibrationCoefficients.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static CameraCalibrationCoefficients get2019LifeCamCoeffs(boolean testMode) {
        return getCoeffs(LIFECAM_240P_CAL_FILE, testMode);
    }

    public static CameraCalibrationCoefficients get2020LifeCamCoeffs(boolean testMode) {
        return getCoeffs(LIFECAM_480P_CAL_FILE, testMode);
    }

    public static CameraCalibrationCoefficients getLaptop() {
        return getCoeffs("laptop.json", true);
    }

    private static int DefaultTimeoutMillis = 5000;

    public static void showImage(Mat frame, String title, int timeoutMs) {
        if (frame.empty()) return;
        try {
            HighGui.imshow(title, frame);
            HighGui.waitKey(timeoutMs);
            HighGui.destroyAllWindows();
        } catch (HeadlessException ignored) {
        }
    }

    public static void showImage(Mat frame, int timeoutMs) {
        showImage(frame, "", timeoutMs);
    }

    public static void showImage(Mat frame, String title) {
        showImage(frame, title, DefaultTimeoutMillis);
    }

    public static void showImage(Mat frame) {
        showImage(frame, DefaultTimeoutMillis);
    }

    public static Path getTestMode2023ImagePath() {
        return getResourcesFolderPath(true)
                .resolve("testimages")
                .resolve(WPI2022Image.kTerminal22ft6in.path);
    }

    public static CameraCalibrationCoefficients get2023LifeCamCoeffs(boolean testMode) {
        return getCoeffs(LIFECAM_1280P_CAL_FILE, testMode);
    }
}
