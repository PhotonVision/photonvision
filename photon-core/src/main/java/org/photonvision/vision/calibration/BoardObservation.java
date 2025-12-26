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

package org.photonvision.vision.calibration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.math.geometry.Pose3d;
import java.awt.Color;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.ColorHelper;

// Ignore the previous calibration data that was stored in the json file.
@JsonIgnoreProperties(ignoreUnknown = true)
public final class BoardObservation implements Cloneable {
    // Expected feature 3d location in the camera frame
    @JsonProperty("locationInObjectSpace")
    public List<Point3> locationInObjectSpace;

    // Observed location in pixel space
    @JsonProperty("locationInImageSpace")
    public List<Point> locationInImageSpace;

    // (measured location in pixels) - (expected from FK)
    @JsonProperty("reprojectionErrors")
    public List<Point> reprojectionErrors;

    // Solver optimized board poses
    @JsonProperty("optimisedCameraToObject")
    public Pose3d optimisedCameraToObject;

    // If we should use this observation when re-calculating camera calibration
    @JsonProperty("cornersUsed")
    public boolean[] cornersUsed;

    @JsonProperty("snapshotName")
    public String snapshotName;

    @JsonProperty("snapshotDataLocation")
    @Nullable
    public Path snapshotDataLocation;

    @JsonCreator
    public BoardObservation(
            @JsonProperty("locationInObjectSpace") List<Point3> locationInObjectSpace,
            @JsonProperty("locationInImageSpace") List<Point> locationInImageSpace,
            @JsonProperty("reprojectionErrors") List<Point> reprojectionErrors,
            @JsonProperty("optimisedCameraToObject") Pose3d optimisedCameraToObject,
            @JsonProperty("cornersUsed") boolean[] cornersUsed,
            @JsonProperty("snapshotName") String snapshotName,
            @JsonProperty("snapshotDataLocation") Path snapshotDataLocation) {
        this.locationInObjectSpace = locationInObjectSpace;
        this.locationInImageSpace = locationInImageSpace;
        this.reprojectionErrors = reprojectionErrors;
        this.optimisedCameraToObject = optimisedCameraToObject;
        this.snapshotName = snapshotName;
        this.snapshotDataLocation = snapshotDataLocation;

        // legacy migration -- we assume all points are inliers
        if (cornersUsed == null) {
            cornersUsed = new boolean[locationInObjectSpace.size()];
            Arrays.fill(cornersUsed, true);
        }
        this.cornersUsed = cornersUsed;
    }

    @Override
    public String toString() {
        return "BoardObservation [locationInObjectSpace="
                + locationInObjectSpace
                + ", locationInImageSpace="
                + locationInImageSpace
                + ", reprojectionErrors="
                + reprojectionErrors
                + ", optimisedCameraToObject="
                + optimisedCameraToObject
                + ", cornersUsed="
                + cornersUsed
                + ", snapshotName="
                + snapshotName
                + ", snapshotDataLocation="
                + snapshotDataLocation
                + "]";
    }

    @Override
    public BoardObservation clone() {
        try {
            return (BoardObservation) super.clone();
        } catch (CloneNotSupportedException e) {
            System.err.println("Guhhh clone buh");
            return null;
        }
    }

    @JsonIgnore
    /**
     * Load the captured board image from disk. Allocates a new Mat, which the caller is responsible
     * for releasing.
     *
     * @return The loaded image, or null if it could not be loaded.
     */
    public Mat loadImage() {
        Mat img = Imgcodecs.imread(this.snapshotDataLocation.toString());
        if (img == null || img.empty() || img.rows() == 0 || img.cols() == 0) {
            return null;
        }

        return img;
    }

    /**
     * Annotate the image with the detected corners, green for used, red for unused
     *
     * @return Annotated image, or null if the image could not be loaded. Caller is responsible for
     *     releasing the Mat.
     */
    @JsonIgnore
    public Mat annotateImage() {
        var image = loadImage();

        if (image == null) {
            return null;
        }

        int thickness = Core.FILLED;
        var diag = Math.hypot(image.width(), image.height());
        int r = (int) Math.max(diag * 4.0 / 500.0, 3);
        for (int i = 0; i < this.locationInImageSpace.size(); i++) {
            var c = locationInImageSpace.get(i);

            // -1, -1 means unused corner
            if (c.x < 0 || c.y < 0) {
                continue;
            }

            Scalar color;
            if (cornersUsed[i]) {
                color = ColorHelper.colorToScalar(Color.green);
            } else {
                color = ColorHelper.colorToScalar(Color.red);
            }
            Imgproc.circle(image, c, r, color, thickness);
        }

        return image;
    }

    /**
     * Mean reprojection error for this observation, skipping corners marked as unused. The overall
     * mean is calculated as the mean of each individual corner's reprojection error, or the distance
     * in pixels between the observed and expected location.
     *
     * @return Mean reprojection error in pixels.
     */
    @JsonIgnore
    double meanReprojectionError() {
        return reprojectionErrors.stream()
                .filter(pt -> cornersUsed[reprojectionErrors.indexOf(pt)])
                .mapToDouble(pt -> Math.hypot(pt.x, pt.y))
                .average()
                .orElse(0);
    }
}
