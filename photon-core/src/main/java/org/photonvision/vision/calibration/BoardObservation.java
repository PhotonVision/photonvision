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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.math.geometry.Pose3d;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.opencv.core.Point;
import org.opencv.core.Point3;

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
    @JsonProperty("includeObservationInCalibration")
    public boolean includeObservationInCalibration;

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
            @JsonProperty("includeObservationInCalibration") boolean includeObservationInCalibration,
            @JsonProperty("snapshotName") String snapshotName,
            @JsonProperty("snapshotDataLocation") Path snapshotDataLocation) {
        this.locationInObjectSpace = locationInObjectSpace;
        this.locationInImageSpace = locationInImageSpace;
        this.reprojectionErrors = reprojectionErrors;
        this.optimisedCameraToObject = optimisedCameraToObject;
        this.includeObservationInCalibration = includeObservationInCalibration;
        this.snapshotName = snapshotName;
        this.snapshotDataLocation = snapshotDataLocation;
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
                + ", includeObservationInCalibration="
                + includeObservationInCalibration
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
}
