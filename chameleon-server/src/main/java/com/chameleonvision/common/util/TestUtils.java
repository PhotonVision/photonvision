package com.chameleonvision.common.util;

import edu.wpi.cscore.CameraServerCvJNI;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;

public class TestUtils {

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
        kCargoStraightDark90in(2.286);

        public static double FOV = 68.5;

        public final double distanceMeters;
        public final String path;

        String getPath() {
            var filename = this.toString().substring(1);
            return "\\2019\\WPI\\" + filename + ".jpg";
        }

        WPI2019Image(double distanceMeters) {
            this.distanceMeters = distanceMeters;
            this.path = getPath();
        }
    }

    public enum WPI2020Image {
        kBlueGoal_060in_Center(1.524),
        kBlueGoal_084in_Center(2.1336),
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
        public final String path;

        String getPath() {
            var filename = this.toString().substring(1).replace('_', '-');
            return "\\2020\\WPI\\" + filename + ".jpg";
        }

        WPI2020Image(double distanceMeters) {
            this.distanceMeters = distanceMeters;
            this.path = getPath();
        }
    }

    private static Path getTestImagesPath() {
        var folder = TestUtils.class.getClassLoader().getResource("testimages");
        return Optional.ofNullable(folder).map(url -> new File(url.getFile()).toPath()).orElse(null);
    }

    public static Path getCalibrationPath() {
        var folder = TestUtils.class.getClassLoader().getResource("calibration");
        return Optional.ofNullable(folder).map(url -> new File(url.getFile()).toPath()).orElse(null);
    }

    public static Path getWPIImagePath(WPI2020Image image) {
        return Path.of(getTestImagesPath().toString(), image.path);
    }

    public static Path getWPIImagePath(WPI2019Image image) {
        return Path.of(getTestImagesPath().toString(), image.path);
    }

    public static void loadLibraries() {
        try {
            CameraServerCvJNI.forceLoad();
        } catch (IOException e) {
            // ignored
        }
    }

    private static int DefaultTimeoutMillis = 5000;

    public static void showImage(Mat frame, String title, int timeoutMs) {
        HighGui.imshow(title, frame);
        HighGui.waitKey(timeoutMs);
        HighGui.destroyAllWindows();
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
}
