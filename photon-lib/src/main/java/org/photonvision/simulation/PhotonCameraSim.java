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

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.CvSource;
import edu.wpi.first.cscore.VideoMode.PixelFormat;
import edu.wpi.first.cscore.VideoSource.ConnectionStrategy;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.Pair;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.util.RuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonTargetSortMode;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.networktables.NTTopicSet;
import org.photonvision.estimation.CameraTargetRelation;
import org.photonvision.estimation.OpenCVHelp;
import org.photonvision.targeting.PNPResults;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;

/**
 * A handle for simulating {@link PhotonCamera} values. Processing simulated targets through this
 * class will change the associated PhotonCamera's results.
 */
@SuppressWarnings("unused")
public class PhotonCameraSim implements AutoCloseable {
    private final PhotonCamera cam;

    NTTopicSet ts = new NTTopicSet();
    private long heartbeatCounter = 0;

    /** This simulated camera's {@link SimCameraProperties} */
    public final SimCameraProperties prop;

    private long nextNTEntryTime = WPIUtilJNI.now();

    private double maxSightRangeMeters = Double.MAX_VALUE;
    private static final double kDefaultMinAreaPx = 100;
    private double minTargetAreaPercent;
    private PhotonTargetSortMode sortMode = PhotonTargetSortMode.Largest;

    // video stream simulation
    private final CvSource videoSimRaw;
    private final Mat videoSimFrameRaw = new Mat();
    private boolean videoSimRawEnabled = true;
    private final CvSource videoSimProcessed;
    private final Mat videoSimFrameProcessed = new Mat();
    private boolean videoSimProcEnabled = true;

    static {
        try {
            var loader =
                    new RuntimeLoader<>(
                            Core.NATIVE_LIBRARY_NAME, RuntimeLoader.getDefaultExtractionRoot(), Core.class);
            loader.loadLibrary();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load native libraries!", e);
        }
    }

    @Override
    public void close() {
        videoSimRaw.close();
        videoSimFrameRaw.release();
        videoSimProcessed.close();
        videoSimFrameProcessed.release();
    }

    /**
     * Constructs a handle for simulating {@link PhotonCamera} values. Processing simulated targets
     * through this class will change the associated PhotonCamera's results.
     *
     * <p><b>This constructor's camera has a 90 deg FOV with no simulated lag!</b>
     *
     * <p>By default, the minimum target area is 100 pixels and there is no maximum sight range.
     *
     * @param camera The camera to be simulated
     */
    public PhotonCameraSim(PhotonCamera camera) {
        this(camera, SimCameraProperties.PERFECT_90DEG());
    }

    /**
     * Constructs a handle for simulating {@link PhotonCamera} values. Processing simulated targets
     * through this class will change the associated PhotonCamera's results.
     *
     * <p>By default, the minimum target area is 100 pixels and there is no maximum sight range.
     *
     * @param camera The camera to be simulated
     * @param prop Properties of this camera such as FOV and FPS
     */
    public PhotonCameraSim(PhotonCamera camera, SimCameraProperties prop) {
        this.cam = camera;
        this.prop = prop;
        setMinTargetAreaPixels(kDefaultMinAreaPx);

        videoSimRaw =
                CameraServer.putVideo(camera.getName() + "-raw", prop.getResWidth(), prop.getResHeight());
        videoSimRaw.setPixelFormat(PixelFormat.kGray);
        videoSimProcessed =
                CameraServer.putVideo(
                        camera.getName() + "-processed", prop.getResWidth(), prop.getResHeight());

        ts.removeEntries();
        ts.subTable = camera.getCameraTable();
        ts.updateEntries();
    }

    /**
     * Constructs a handle for simulating {@link PhotonCamera} values. Processing simulated targets
     * through this class will change the associated PhotonCamera's results.
     *
     * @param camera The camera to be simulated
     * @param prop Properties of this camera such as FOV and FPS
     * @param minTargetAreaPercent The minimum percentage(0 - 100) a detected target must take up of
     *     the camera's image to be processed. Match this with your contour filtering settings in the
     *     PhotonVision GUI.
     * @param maxSightRangeMeters Maximum distance at which the target is illuminated to your camera.
     *     Note that minimum target area of the image is separate from this.
     */
    public PhotonCameraSim(
            PhotonCamera camera,
            SimCameraProperties prop,
            double minTargetAreaPercent,
            double maxSightRangeMeters) {
        this(camera, prop);
        this.minTargetAreaPercent = minTargetAreaPercent;
        this.maxSightRangeMeters = maxSightRangeMeters;
    }

    public PhotonCamera getCamera() {
        return cam;
    }

    public double getMinTargetAreaPercent() {
        return minTargetAreaPercent;
    }

    public double getMinTargetAreaPixels() {
        return minTargetAreaPercent / 100.0 * prop.getResArea();
    }

    public double getMaxSightRangeMeters() {
        return maxSightRangeMeters;
    }

    public PhotonTargetSortMode getTargetSortMode() {
        return sortMode;
    }

    public CvSource getVideoSimRaw() {
        return videoSimRaw;
    }

    public Mat getVideoSimFrameRaw() {
        return videoSimFrameRaw;
    }

    /**
     * Determines if this target's pose should be visible to the camera without considering its
     * projected image points. Does not account for image area.
     *
     * @param camPose Camera's 3d pose
     * @param target Vision target containing pose and shape
     * @return If this vision target can be seen before image projection.
     */
    public boolean canSeeTargetPose(Pose3d camPose, VisionTargetSim target) {
        var rel = new CameraTargetRelation(camPose, target.getPose());
        return (
        // target translation is outside of camera's FOV
        (Math.abs(rel.camToTargYaw.getDegrees()) < prop.getHorizFOV().getDegrees() / 2)
                && (Math.abs(rel.camToTargPitch.getDegrees()) < prop.getVertFOV().getDegrees() / 2)
                && (!target.getModel().isPlanar
                        || Math.abs(rel.targToCamAngle.getDegrees())
                                < 90) // camera is behind planar target and it should be occluded
                && (rel.camToTarg.getTranslation().getNorm() <= maxSightRangeMeters)); // target is too far
    }

    /**
     * Determines if all target corners are inside the camera's image.
     *
     * @param corners The corners of the target as image points(x,y)
     */
    public boolean canSeeCorners(List<TargetCorner> corners) {
        // corner is outside of resolution
        for (var corner : corners) {
            if (MathUtil.clamp(corner.x, 0, prop.getResWidth()) != corner.x
                    || MathUtil.clamp(corner.y, 0, prop.getResHeight()) != corner.y) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine if this camera should process a new frame based on performance metrics and the time
     * since the last update. This returns an Optional which is either empty if no update should occur
     * or a Long of the timestamp in microseconds of when the frame which should be received by NT. If
     * a timestamp is returned, the last frame update time becomes that timestamp.
     *
     * @return Optional long which is empty while blocked or the NT entry timestamp in microseconds if
     *     ready
     */
    public Optional<Long> consumeNextEntryTime() {
        // check if this camera is ready for another frame update
        long now = WPIUtilJNI.now();
        long timestamp = -1;
        int iter = 0;
        // prepare next latest update
        while (now >= nextNTEntryTime) {
            timestamp = nextNTEntryTime;
            long frameTime = (long) (prop.estMsUntilNextFrame() * 1e3);
            nextNTEntryTime += frameTime;

            // if frame time is very small, avoid blocking
            if (iter++ > 50) {
                timestamp = now;
                nextNTEntryTime = now + frameTime;
                break;
            }
        }
        // return the timestamp of the latest update
        if (timestamp >= 0) return Optional.of(timestamp);
        // or this camera isn't ready to process yet
        else return Optional.empty();
    }

    /**
     * The minimum percentage(0 - 100) a detected target must take up of the camera's image to be
     * processed.
     */
    public void setMinTargetAreaPercent(double areaPercent) {
        this.minTargetAreaPercent = areaPercent;
    }

    /**
     * The minimum number of pixels a detected target must take up in the camera's image to be
     * processed.
     */
    public void setMinTargetAreaPixels(double areaPx) {
        this.minTargetAreaPercent = areaPx / prop.getResArea() * 100;
    }

    /**
     * Maximum distance at which the target is illuminated to your camera. Note that minimum target
     * area of the image is separate from this.
     */
    public void setMaxSightRange(double rangeMeters) {
        this.maxSightRangeMeters = rangeMeters;
    }

    /** Defines the order the targets are sorted in the pipeline result. */
    public void setTargetSortMode(PhotonTargetSortMode sortMode) {
        if (sortMode != null) this.sortMode = sortMode;
    }

    /** Sets whether the raw video stream simulation is enabled. */
    public void enableRawStream(boolean enabled) {
        videoSimRawEnabled = enabled;
    }

    /** Sets whether the processed video stream simulation is enabled. */
    public void enableProcessedStream(boolean enabled) {
        videoSimProcEnabled = enabled;
    }

    public PhotonPipelineResult process(
            double latencyMillis, Pose3d cameraPose, List<VisionTargetSim> targets) {
        // sort targets by distance to camera
        targets = new ArrayList<>(targets);
        targets.sort(
                (t1, t2) -> {
                    double dist1 = t1.getPose().getTranslation().getDistance(cameraPose.getTranslation());
                    double dist2 = t2.getPose().getTranslation().getDistance(cameraPose.getTranslation());
                    if (dist1 == dist2) return 0;
                    return dist1 < dist2 ? 1 : -1;
                });
        // all targets visible (in FOV)
        var visibleTags = new ArrayList<Pair<Integer, List<TargetCorner>>>();
        // all targets actually detectable to the camera
        var detectableTgts = new ArrayList<PhotonTrackedTarget>();

        // reset our frame
        VideoSimUtil.updateVideoProp(videoSimRaw, prop);
        VideoSimUtil.updateVideoProp(videoSimProcessed, prop);
        Size videoFrameSize = new Size(prop.getResWidth(), prop.getResHeight());
        Mat.zeros(videoFrameSize, CvType.CV_8UC1).assignTo(videoSimFrameRaw);

        for (var tgt : targets) {
            // pose isn't visible, skip to next
            if (!canSeeTargetPose(cameraPose, tgt)) continue;

            // find target's 3d corner points
            // TODO: Handle spherical targets
            var fieldCorners = tgt.getFieldVertices();

            // project 3d target points into 2d image points
            var targetCorners =
                    OpenCVHelp.projectPoints(
                            prop.getIntrinsics(), prop.getDistCoeffs(), cameraPose, fieldCorners);
            // save visible tags for stream simulation
            if (tgt.fiducialID >= 0) {
                visibleTags.add(new Pair<>(tgt.fiducialID, targetCorners));
            }
            // estimate pixel noise
            var noisyTargetCorners = prop.estPixelNoise(targetCorners);
            // find the (naive) 2d yaw/pitch
            var centerPt = OpenCVHelp.getMinAreaRect(noisyTargetCorners).center;
            var centerRot = prop.getPixelRot(new TargetCorner(centerPt.x, centerPt.y));
            // find contour area
            double areaPercent = prop.getContourAreaPercent(noisyTargetCorners);

            // projected target can't be detected, skip to next
            if (!(canSeeCorners(noisyTargetCorners) && areaPercent >= minTargetAreaPercent)) continue;

            var pnpSim = new PNPResults();
            if (tgt.fiducialID >= 0 && tgt.getFieldVertices().size() == 4) { // single AprilTag solvePNP
                pnpSim =
                        OpenCVHelp.solvePNP_SQUARE(
                                prop.getIntrinsics(),
                                prop.getDistCoeffs(),
                                tgt.getModel().vertices,
                                noisyTargetCorners);
                if (!pnpSim.isPresent) continue;
                centerRot =
                        prop.getPixelRot(
                                OpenCVHelp.projectPoints(
                                                prop.getIntrinsics(),
                                                prop.getDistCoeffs(),
                                                new Pose3d(),
                                                List.of(pnpSim.best.getTranslation()))
                                        .get(0));
            }

            Point[] minAreaRectPts = new Point[noisyTargetCorners.size()];
            OpenCVHelp.getMinAreaRect(noisyTargetCorners).points(minAreaRectPts);

            detectableTgts.add(
                    new PhotonTrackedTarget(
                            Math.toDegrees(centerRot.getZ()),
                            -Math.toDegrees(centerRot.getY()),
                            areaPercent,
                            Math.toDegrees(centerRot.getX()),
                            tgt.fiducialID,
                            pnpSim.best,
                            pnpSim.alt,
                            pnpSim.ambiguity,
                            List.of(OpenCVHelp.pointsToTargetCorners(minAreaRectPts)),
                            noisyTargetCorners));
        }
        // render visible tags to raw video frame
        if (videoSimRawEnabled) {
            for (var tag : visibleTags) {
                VideoSimUtil.warp16h5TagImage(
                        tag.getFirst(), OpenCVHelp.targetCornersToMat(tag.getSecond()), videoSimFrameRaw, true);
            }
            videoSimRaw.putFrame(videoSimFrameRaw);
        } else videoSimRaw.setConnectionStrategy(ConnectionStrategy.kForceClose);
        // draw/annotate tag detection outline on processed view
        if (videoSimProcEnabled) {
            Imgproc.cvtColor(videoSimFrameRaw, videoSimFrameProcessed, Imgproc.COLOR_GRAY2BGR);
            for (var tgt : detectableTgts) {
                if (tgt.getFiducialId() >= 0) {
                    VideoSimUtil.drawTagDetection(
                            tgt.getFiducialId(),
                            OpenCVHelp.targetCornersToMat(tgt.getDetectedCorners()),
                            videoSimFrameProcessed);
                }
            }
            videoSimProcessed.putFrame(videoSimFrameProcessed);
        } else videoSimProcessed.setConnectionStrategy(ConnectionStrategy.kForceClose);

        // put this simulated data to NT
        if (sortMode != null) {
            detectableTgts.sort(sortMode.getComparator());
        }
        return new PhotonPipelineResult(latencyMillis, detectableTgts);
    }

    /**
     * Simulate one processed frame of vision data, putting one result to NT at current timestamp.
     * Image capture time is assumed be (current time - latency).
     *
     * @param result The pipeline result to submit
     */
    public void submitProcessedFrame(PhotonPipelineResult result) {
        submitProcessedFrame(result, WPIUtilJNI.now());
    }

    /**
     * Simulate one processed frame of vision data, putting one result to NT. Image capture timestamp
     * overrides {@link PhotonPipelineResult#getTimestampSeconds() getTimestampSeconds()} for more
     * precise latency simulation.
     *
     * @param result The pipeline result to submit
     * @param receiveTimestamp The (sim) timestamp when this result was read by NT in microseconds
     */
    public void submitProcessedFrame(PhotonPipelineResult result, long receiveTimestamp) {
        ts.latencyMillisEntry.set(result.getLatencyMillis(), receiveTimestamp);

        var newPacket = new Packet(result.getPacketSize());
        result.populatePacket(newPacket);
        ts.rawBytesEntry.set(newPacket.getData(), receiveTimestamp);

        boolean hasTargets = result.hasTargets();
        ts.hasTargetEntry.set(hasTargets, receiveTimestamp);
        if (!hasTargets) {
            ts.targetPitchEntry.set(0.0, receiveTimestamp);
            ts.targetYawEntry.set(0.0, receiveTimestamp);
            ts.targetAreaEntry.set(0.0, receiveTimestamp);
            ts.targetPoseEntry.set(new double[] {0.0, 0.0, 0.0}, receiveTimestamp);
            ts.targetSkewEntry.set(0.0, receiveTimestamp);
        } else {
            var bestTarget = result.getBestTarget();

            ts.targetPitchEntry.set(bestTarget.getPitch(), receiveTimestamp);
            ts.targetYawEntry.set(bestTarget.getYaw(), receiveTimestamp);
            ts.targetAreaEntry.set(bestTarget.getArea(), receiveTimestamp);
            ts.targetSkewEntry.set(bestTarget.getSkew(), receiveTimestamp);

            var transform = bestTarget.getBestCameraToTarget();
            double[] poseData = {
                transform.getX(), transform.getY(), transform.getRotation().toRotation2d().getDegrees()
            };
            ts.targetPoseEntry.set(poseData, receiveTimestamp);
        }

        ts.cameraIntrinsicsPublisher.set(prop.getIntrinsics().getData(), receiveTimestamp);
        ts.cameraDistortionPublisher.set(prop.getDistCoeffs().getData(), receiveTimestamp);
        ts.heartbeatPublisher.set(heartbeatCounter++, receiveTimestamp);
    }
}
