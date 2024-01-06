/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.photonvision.simulation;

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.interpolation.TimeInterpolatableBuffer;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.photonvision.PhotonCamera;
import org.photonvision.estimation.TargetModel;

/**
 * A simulated vision system involving a camera(s) and coprocessor(s) mounted on a mobile robot
 * running PhotonVision, detecting targets placed on the field. {@link VisionTargetSim}s added to
 * this class will be detected by the {@link PhotonCameraSim}s added to this class. This class
 * should be updated periodically with the robot's current pose in order to publish the simulated
 * camera target info.
 */
public class VisionSystemSim {
    private final Map<String, PhotonCameraSim> camSimMap = new HashMap<>();
    private static final double kBufferLengthSeconds = 1.5;
    // save robot-to-camera for each camera over time (Pose3d is easily interpolatable)
    private final Map<PhotonCameraSim, TimeInterpolatableBuffer<Pose3d>> camTrfMap = new HashMap<>();

    // interpolate drivetrain with twists
    private final TimeInterpolatableBuffer<Pose3d> robotPoseBuffer =
            TimeInterpolatableBuffer.createBuffer(kBufferLengthSeconds);

    private final Map<String, Set<VisionTargetSim>> targetSets = new HashMap<>();

    private final Field2d dbgField;

    private final Transform3d kEmptyTrf = new Transform3d();

    /**
     * A simulated vision system involving a camera(s) and coprocessor(s) mounted on a mobile robot
     * running PhotonVision, detecting targets placed on the field. {@link VisionTargetSim}s added to
     * this class will be detected by the {@link PhotonCameraSim}s added to this class. This class
     * should be updated periodically with the robot's current pose in order to publish the simulated
     * camera target info.
     *
     * @param visionSystemName The specific identifier for this vision system in NetworkTables.
     */
    public VisionSystemSim(String visionSystemName) {
        dbgField = new Field2d();
        String tableName = "VisionSystemSim-" + visionSystemName;
        SmartDashboard.putData(tableName + "/Sim Field", dbgField);
    }

    /** Get one of the simulated cameras. */
    public Optional<PhotonCameraSim> getCameraSim(String name) {
        return Optional.ofNullable(camSimMap.get(name));
    }

    /** Get all the simulated cameras. */
    public Collection<PhotonCameraSim> getCameraSims() {
        return camSimMap.values();
    }

    /**
     * Adds a simulated camera to this vision system with a specified robot-to-camera transformation.
     * The vision targets registered with this vision system simulation will be observed by the
     * simulated {@link PhotonCamera}.
     *
     * @param cameraSim The camera simulation
     * @param robotToCamera The transform from the robot pose to the camera pose
     */
    public void addCamera(PhotonCameraSim cameraSim, Transform3d robotToCamera) {
        var existing = camSimMap.putIfAbsent(cameraSim.getCamera().getName(), cameraSim);
        if (existing == null) {
            camTrfMap.put(cameraSim, TimeInterpolatableBuffer.createBuffer(kBufferLengthSeconds));
            camTrfMap
                    .get(cameraSim)
                    .addSample(Timer.getFPGATimestamp(), new Pose3d().plus(robotToCamera));
        }
    }

    /** Remove all simulated cameras from this vision system. */
    public void clearCameras() {
        camSimMap.clear();
        camTrfMap.clear();
    }

    /**
     * Remove a simulated camera from this vision system.
     *
     * @return If the camera was present and removed
     */
    public boolean removeCamera(PhotonCameraSim cameraSim) {
        boolean success = camSimMap.remove(cameraSim.getCamera().getName()) != null;
        camTrfMap.remove(cameraSim);
        return success;
    }

    /**
     * Get a simulated camera's position relative to the robot. If the requested camera is invalid, an
     * empty optional is returned.
     *
     * @param cameraSim The specific camera to get the robot-to-camera transform of
     * @return The transform of this camera, or an empty optional if it is invalid
     */
    public Optional<Transform3d> getRobotToCamera(PhotonCameraSim cameraSim) {
        return getRobotToCamera(cameraSim, Timer.getFPGATimestamp());
    }

    /**
     * Get a simulated camera's position relative to the robot. If the requested camera is invalid, an
     * empty optional is returned.
     *
     * @param cameraSim The specific camera to get the robot-to-camera transform of
     * @param timeSeconds Timestamp in seconds of when the transform should be observed
     * @return The transform of this camera, or an empty optional if it is invalid
     */
    public Optional<Transform3d> getRobotToCamera(PhotonCameraSim cameraSim, double timeSeconds) {
        var trfBuffer = camTrfMap.get(cameraSim);
        if (trfBuffer == null) return Optional.empty();
        var sample = trfBuffer.getSample(timeSeconds);
        if (sample.isEmpty()) return Optional.empty();
        return Optional.of(new Transform3d(new Pose3d(), sample.orElse(new Pose3d())));
    }

    /**
     * Get a simulated camera's position on the field. If the requested camera is invalid, an empty
     * optional is returned.
     *
     * @param cameraSim The specific camera to get the field pose of
     * @return The pose of this camera, or an empty optional if it is invalid
     */
    public Optional<Pose3d> getCameraPose(PhotonCameraSim cameraSim) {
        return getCameraPose(cameraSim, Timer.getFPGATimestamp());
    }

    /**
     * Get a simulated camera's position on the field. If the requested camera is invalid, an empty
     * optional is returned.
     *
     * @param cameraSim The specific camera to get the field pose of
     * @param timeSeconds Timestamp in seconds of when the pose should be observed
     * @return The pose of this camera, or an empty optional if it is invalid
     */
    public Optional<Pose3d> getCameraPose(PhotonCameraSim cameraSim, double timeSeconds) {
        var robotToCamera = getRobotToCamera(cameraSim, timeSeconds);
        return robotToCamera.map(transform3d -> getRobotPose(timeSeconds).plus(transform3d));
    }

    /**
     * Adjust a camera's position relative to the robot. Use this if your camera is on a gimbal or
     * turret or some other mobile platform.
     *
     * @param cameraSim The simulated camera to change the relative position of
     * @param robotToCamera New transform from the robot to the camera
     * @return If the cameraSim was valid and transform was adjusted
     */
    public boolean adjustCamera(PhotonCameraSim cameraSim, Transform3d robotToCamera) {
        var trfBuffer = camTrfMap.get(cameraSim);
        if (trfBuffer == null) return false;
        trfBuffer.addSample(Timer.getFPGATimestamp(), new Pose3d().plus(robotToCamera));
        return true;
    }

    /** Reset the previous transforms for all cameras to their current transform. */
    public void resetCameraTransforms() {
        for (var cam : camTrfMap.keySet()) resetCameraTransforms(cam);
    }

    /**
     * Reset the transform history for this camera to just the current transform.
     *
     * @return If the cameraSim was valid and transforms were reset
     */
    public boolean resetCameraTransforms(PhotonCameraSim cameraSim) {
        double now = Timer.getFPGATimestamp();
        var trfBuffer = camTrfMap.get(cameraSim);
        if (trfBuffer == null) return false;
        var lastTrf = new Transform3d(new Pose3d(), trfBuffer.getSample(now).orElse(new Pose3d()));
        trfBuffer.clear();
        adjustCamera(cameraSim, lastTrf);
        return true;
    }

    public Set<VisionTargetSim> getVisionTargets() {
        var all = new HashSet<VisionTargetSim>();
        for (var entry : targetSets.entrySet()) {
            all.addAll(entry.getValue());
        }
        return all;
    }

    public Set<VisionTargetSim> getVisionTargets(String type) {
        return targetSets.get(type);
    }

    /**
     * Adds targets on the field which your vision system is designed to detect. The {@link
     * PhotonCamera}s simulated from this system will report the location of the camera relative to
     * the subset of these targets which are visible from the given camera position.
     *
     * <p>By default these are added under the type "targets".
     *
     * @param targets Targets to add to the simulated field
     */
    public void addVisionTargets(VisionTargetSim... targets) {
        addVisionTargets("targets", targets);
    }

    /**
     * Adds targets on the field which your vision system is designed to detect. The {@link
     * PhotonCamera}s simulated from this system will report the location of the camera relative to
     * the subset of these targets which are visible from the given camera position.
     *
     * <p>The AprilTags from this layout will be added as vision targets under the type "apriltag".
     * The poses added preserve the tag layout's current alliance origin. If the tag layout's alliance
     * origin is changed, these added tags will have to be cleared and re-added.
     *
     * @param tagLayout The field tag layout to get Apriltag poses and IDs from
     */
    public void addAprilTags(AprilTagFieldLayout tagLayout) {
        for (AprilTag tag : tagLayout.getTags()) {
            addVisionTargets(
                    "apriltag",
                    new VisionTargetSim(
                            tagLayout.getTagPose(tag.ID).get(), // preserve alliance rotation
                            TargetModel.kAprilTag36h11,
                            tag.ID));
        }
    }

    /**
     * Adds targets on the field which your vision system is designed to detect. The {@link
     * PhotonCamera}s simulated from this system will report the location of the camera relative to
     * the subset of these targets which are visible from the given camera position.
     *
     * @param type Type of target (e.g. "cargo").
     * @param targets Targets to add to the simulated field
     */
    public void addVisionTargets(String type, VisionTargetSim... targets) {
        targetSets.computeIfAbsent(type, k -> new HashSet<>());
        for (var tgt : targets) {
            targetSets.get(type).add(tgt);
        }
    }

    public void clearVisionTargets() {
        targetSets.clear();
    }

    public void clearAprilTags() {
        removeVisionTargets("apriltag");
    }

    public Set<VisionTargetSim> removeVisionTargets(String type) {
        return targetSets.remove(type);
    }

    public Set<VisionTargetSim> removeVisionTargets(VisionTargetSim... targets) {
        var removeList = List.of(targets);
        var removedSet = new HashSet<VisionTargetSim>();
        for (var entry : targetSets.entrySet()) {
            entry
                    .getValue()
                    .removeIf(
                            t -> {
                                if (removeList.contains(t)) {
                                    removedSet.add(t);
                                    return true;
                                } else return false;
                            });
        }
        return removedSet;
    }

    /** Get the latest robot pose in meters saved by the vision system. */
    public Pose3d getRobotPose() {
        return getRobotPose(Timer.getFPGATimestamp());
    }

    /**
     * Get the robot pose in meters saved by the vision system at this timestamp.
     *
     * @param timestamp Timestamp of the desired robot pose
     */
    public Pose3d getRobotPose(double timestamp) {
        return robotPoseBuffer.getSample(timestamp).orElse(new Pose3d());
    }

    /** Clears all previous robot poses and sets robotPose at current time. */
    public void resetRobotPose(Pose2d robotPose) {
        resetRobotPose(new Pose3d(robotPose));
    }

    /** Clears all previous robot poses and sets robotPose at current time. */
    public void resetRobotPose(Pose3d robotPose) {
        robotPoseBuffer.clear();
        robotPoseBuffer.addSample(Timer.getFPGATimestamp(), robotPose);
    }

    public Field2d getDebugField() {
        return dbgField;
    }

    /**
     * Periodic update. Ensure this is called repeatedly-- camera performance is used to automatically
     * determine if a new frame should be submitted.
     *
     * @param robotPoseMeters The simulated robot pose in meters
     */
    public void update(Pose2d robotPoseMeters) {
        update(new Pose3d(robotPoseMeters));
    }

    /**
     * Periodic update. Ensure this is called repeatedly-- camera performance is used to automatically
     * determine if a new frame should be submitted.
     *
     * @param robotPoseMeters The simulated robot pose in meters
     */
    public void update(Pose3d robotPoseMeters) {
        var targetTypes = targetSets.entrySet();
        // update vision targets on field
        targetTypes.forEach(
                entry ->
                        dbgField
                                .getObject(entry.getKey())
                                .setPoses(
                                        entry.getValue().stream()
                                                .map(t -> t.getPose().toPose2d())
                                                .collect(Collectors.toList())));

        if (robotPoseMeters == null) return;

        // save "real" robot poses over time
        double now = Timer.getFPGATimestamp();
        robotPoseBuffer.addSample(now, robotPoseMeters);
        dbgField.setRobotPose(robotPoseMeters.toPose2d());

        var allTargets = new ArrayList<VisionTargetSim>();
        targetTypes.forEach((entry) -> allTargets.addAll(entry.getValue()));
        var visTgtPoses2d = new ArrayList<Pose2d>();
        var cameraPoses2d = new ArrayList<Pose2d>();
        boolean processed = false;
        // process each camera
        for (var camSim : camSimMap.values()) {
            // check if this camera is ready to process and get latency
            var optTimestamp = camSim.consumeNextEntryTime();
            if (optTimestamp.isEmpty()) continue;
            else processed = true;
            // when this result "was" read by NT
            long timestampNT = optTimestamp.get();
            // this result's processing latency in milliseconds
            double latencyMillis = camSim.prop.estLatencyMs();
            // the image capture timestamp in seconds of this result
            double timestampCapture = timestampNT / 1e6 - latencyMillis / 1e3;

            // use camera pose from the image capture timestamp
            Pose3d lateRobotPose = getRobotPose(timestampCapture);
            Pose3d lateCameraPose = lateRobotPose.plus(getRobotToCamera(camSim, timestampCapture).get());
            cameraPoses2d.add(lateCameraPose.toPose2d());

            // process a PhotonPipelineResult with visible targets
            var camResult = camSim.process(latencyMillis, lateCameraPose, allTargets);
            // publish this info to NT at estimated timestamp of receive
            camSim.submitProcessedFrame(camResult, timestampNT);
            // display debug results
            for (var target : camResult.getTargets()) {
                var trf = target.getBestCameraToTarget();
                if (trf.equals(kEmptyTrf)) continue;
                visTgtPoses2d.add(lateCameraPose.transformBy(trf).toPose2d());
            }
        }
        if (processed) dbgField.getObject("visibleTargetPoses").setPoses(visTgtPoses2d);
        if (!cameraPoses2d.isEmpty()) dbgField.getObject("cameras").setPoses(cameraPoses2d);
    }
}
