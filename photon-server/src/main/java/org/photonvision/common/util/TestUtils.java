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

package org.photonvision.common.util;

import edu.wpi.cscore.CameraServerCvJNI;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;

public class TestUtils {

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
        kCargoStraightDark90in(2.286);

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

    private static Path getResourcesFolderPath() {
        return Path.of("src", "test", "resources").toAbsolutePath();
    }

    public static Path getTestImagesPath() {
        return getResourcesFolderPath().resolve("testimages");
    }

    public static Path getCalibrationPath() {
        return getResourcesFolderPath().resolve("calibration");
    }

    public static Path getPowercellPath() {
        return getTestImagesPath().resolve("polygons").resolve("powercells");
    }

    public static Path getWPIImagePath(WPI2020Image image) {
        return getTestImagesPath().resolve(image.path);
    }

    public static Path getWPIImagePath(WPI2019Image image) {
        return getTestImagesPath().resolve(image.path);
    }

    public static Path getPolygonImagePath(PolygonTestImages image) {
        return getTestImagesPath().resolve(image.path);
    }

    public static Path getPowercellImagePath(PowercellTestImages image) {
        return getPowercellPath().resolve(image.path);
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
}
