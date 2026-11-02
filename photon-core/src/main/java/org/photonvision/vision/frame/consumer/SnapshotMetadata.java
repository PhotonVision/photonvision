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

package org.photonvision.vision.frame.consumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.opencv.core.Point;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.pipeline.AdvancedPipelineSettings;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;

public final class SnapshotMetadata {
    private SnapshotMetadata() {}

    /** Must be called before the result's targets are released. */
    public static Map<String, Object> create(CVPipelineResult result, CVPipelineSettings settings) {
        var ret = new LinkedHashMap<String, Object>();

        if (settings != null) {
            var pipeline = new LinkedHashMap<String, Object>();
            pipeline.put("index", settings.pipelineIndex);
            pipeline.put("nickname", settings.pipelineNickname);
            pipeline.put("type", settings.pipelineType.name());
            ret.put("pipeline", pipeline);
        }

        if (result == null) return ret;

        ret.put("sequenceID", result.sequenceID);
        ret.put("imageCaptureTimestampNanos", result.getImageCaptureTimestampNanos());
        ret.put("processingMillis", result.getProcessingMillis());
        ret.put("fps", result.fps);

        var frame = result.inputAndOutputFrame;
        if (frame != null && frame.frameStaticProperties != null) {
            var props = frame.frameStaticProperties;
            var image = new LinkedHashMap<String, Object>();
            image.put("width", props.imageWidth);
            image.put("height", props.imageHeight);
            image.put("fov", props.fov);
            ret.put("image", image);

            if (props.cameraCalibration != null) {
                ret.put("calibration", calibrationToMap(props.cameraCalibration));
            }
        }

        boolean hasPoses = settings instanceof AdvancedPipelineSettings s && s.solvePNPEnabled;
        var targets = new ArrayList<Map<String, Object>>(result.targets.size());
        var fiducialIds = new ArrayList<Integer>();
        for (var t : result.targets) {
            targets.add(targetToMap(t, hasPoses));
            if (t.isFiducial()) fiducialIds.add(t.getFiducialId());
        }
        ret.put("targetCount", targets.size());
        ret.put("fiducialIds", fiducialIds);
        ret.put("targets", targets);

        if (result.objectDetectionClassNames != null && !result.objectDetectionClassNames.isEmpty()) {
            ret.put("classNames", result.objectDetectionClassNames);
        }

        if (result.multiTagResult != null && result.multiTagResult.isPresent()) {
            var mt = result.multiTagResult.get();
            var multitag = new LinkedHashMap<String, Object>();
            multitag.put("fieldToCamera", SerializationUtils.transformToHashMap(mt.estimatedPose.best));
            multitag.put("reprojectionError", mt.estimatedPose.bestReprojErr);
            multitag.put("fiducialIDsUsed", mt.fiducialIDsUsed.stream().map(Short::intValue).toList());
            ret.put("multitagResult", multitag);
        }

        return ret;
    }

    private static Map<String, Object> targetToMap(TrackedTarget t, boolean hasPoses) {
        var ret = new LinkedHashMap<String, Object>();
        ret.put("fiducialId", t.getFiducialId());
        ret.put("classId", t.getClassID());
        ret.put("confidence", t.getConfidence());
        ret.put("yaw", t.getYaw());
        ret.put("pitch", t.getPitch());
        ret.put("skew", t.getSkew());
        ret.put("area", t.getArea());
        ret.put("poseAmbiguity", t.getPoseAmbiguity());

        if (hasPoses) {
            ret.put(
                    "bestCameraToTarget", SerializationUtils.transformToHashMap(t.getBestCameraToTarget3d()));
            ret.put(
                    "altCameraToTarget", SerializationUtils.transformToHashMap(t.getAltCameraToTarget3d()));
        }

        var corners = t.getTargetCorners();
        if (corners != null) ret.put("detectedCorners", pointsToList(corners));

        return ret;
    }

    private static Map<String, Object> calibrationToMap(CameraCalibrationCoefficients cal) {
        var ret = new LinkedHashMap<String, Object>();
        ret.put("width", cal.resolution.width);
        ret.put("height", cal.resolution.height);
        ret.put("lensModel", cal.lensmodel.name());
        // Row-major 3x3: [fx, 0, cx, 0, fy, cy, 0, 0, 1]
        ret.put("cameraIntrinsics", Arrays.stream(cal.getIntrinsicsArr()).boxed().toList());
        ret.put("distCoeffs", Arrays.stream(cal.getDistCoeffsArr()).boxed().toList());
        return ret;
    }

    private static List<Map<String, Object>> pointsToList(List<Point> points) {
        var ret = new ArrayList<Map<String, Object>>(points.size());
        for (var p : points) {
            ret.add(Map.of("x", p.x, "y", p.y));
        }
        return ret;
    }
}
