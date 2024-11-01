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

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.CvSource;
import edu.wpi.first.cscore.VideoSource.ConnectionStrategy;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.Pair;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.util.PixelFormat;
import edu.wpi.first.util.WPIUtilJNI;
import edu.wpi.first.wpilibj.RobotController;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonTargetSortMode;
import org.photonvision.common.networktables.NTTopicSet;
import org.photonvision.estimation.CameraTargetRelation;
import org.photonvision.estimation.OpenCVHelp;
import org.photonvision.estimation.RotTrlTransform3d;
import org.photonvision.estimation.TargetModel;
import org.photonvision.estimation.VisionEstimation;
import org.photonvision.targeting.MultiTargetPNPResult;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.PnpResult;

/**
 * A handle for simulating {@link PhotonCamera} values. Processing simulated targets through this
 * class will change the associated PhotonCamera's results.
 */
@SuppressWarnings("unused")
public class PhotonCameraSim implements AutoCloseable {
    private final PhotonCamera cam;

    protected NTTopicSet ts = new NTTopicSet();
    private long heartbeatCounter = 1;

    /** This simulated camera's {@link SimCameraProperties} */
    public final SimCameraProperties prop;

    private long nextNTEntryTime = WPIUtilJNI.now();

    private double maxSightRangeMeters = Double.MAX_VALUE;
    private static final double kDefaultMinAreaPx = 100;
    private double minTargetAreaPercent;
    private PhotonTargetSortMode sortMode = PhotonTargetSortMode.Largest;

    private final AprilTagFieldLayout tagLayout =
            AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField);

    // video stream simulation
    private final CvSource videoSimRaw;
    private final Mat videoSimFrameRaw = new Mat();
    private boolean videoSimRawEnabled = true;
    private boolean videoSimWireframeEnabled = false;
    private double videoSimWireframeResolution = 0.1;
    private final CvSource videoSimProcessed;
    private final Mat videoSimFrameProcessed = new Mat();
    private boolean videoSimProcEnabled = true;

    static {
        OpenCVHelp.forceLoadOpenCV();
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
     * Determines if all target points are inside the camera's image.
     *
     * @param points The target's 2d image points
     */
    public boolean canSeeCorners(Point[] points) {
        for (var point : points) {
            if (MathUtil.clamp(point.x, 0, prop.getResWidth()) != point.x
                    || MathUtil.clamp(point.y, 0, prop.getResHeight()) != point.y) {
                return false; // point is outside of resolution
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

    /**
     * Sets whether the raw video stream simulation is enabled.
     *
     * <p>Note: This may increase loop times.
     */
    public void enableRawStream(boolean enabled) {
        videoSimRawEnabled = enabled;
    }

    /**
     * Sets whether a wireframe of the field is drawn to the raw video stream.
     *
     * <p>Note: This will dramatically increase loop times.
     */
    public void enableDrawWireframe(boolean enabled) {
        videoSimWireframeEnabled = enabled;
    }

    /**
     * Sets the resolution of the drawn wireframe if enabled. Drawn line segments will be subdivided
     * into smaller segments based on a threshold set by the resolution.
     *
     * @param resolution Resolution as a fraction(0 - 1) of the video frame's diagonal length in
     *     pixels
     */
    public void setWireframeResolution(double resolution) {
        videoSimWireframeResolution = resolution;
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
        // all targets visible before noise
        var visibleTgts = new ArrayList<Pair<VisionTargetSim, Point[]>>();
        // all targets actually detected by camera (after noise)
        var detectableTgts = new ArrayList<PhotonTrackedTarget>();
        // basis change from world coordinates to camera coordinates
        var camRt = RotTrlTransform3d.makeRelativeTo(cameraPose);

        // reset our frame
        VideoSimUtil.updateVideoProp(videoSimRaw, prop);
        VideoSimUtil.updateVideoProp(videoSimProcessed, prop);
        Size videoFrameSize = new Size(prop.getResWidth(), prop.getResHeight());
        Mat.zeros(videoFrameSize, CvType.CV_8UC1).assignTo(videoSimFrameRaw);

        for (var tgt : targets) {
            // pose isn't visible, skip to next
            if (!canSeeTargetPose(cameraPose, tgt)) continue;

            // find target's 3d corner points
            var fieldCorners = tgt.getFieldVertices();
            if (tgt.getModel().isSpherical) { // target is spherical
                var model = tgt.getModel();
                // orient the model to the camera (like a sprite/decal) so it appears similar regardless of
                // view
                fieldCorners =
                        model.getFieldVertices(
                                TargetModel.getOrientedPose(
                                        tgt.getPose().getTranslation(), cameraPose.getTranslation()));
            }
            // project 3d target points into 2d image points
            var imagePoints =
                    OpenCVHelp.projectPoints(prop.getIntrinsics(), prop.getDistCoeffs(), camRt, fieldCorners);
            // spherical targets need a rotated rectangle of their midpoints for visualization
            if (tgt.getModel().isSpherical) {
                var center = OpenCVHelp.avgPoint(imagePoints);
                int l = 0, t, b, r = 0;
                // reference point (left side midpoint)
                for (int i = 1; i < 4; i++) {
                    if (imagePoints[i].x < imagePoints[l].x) l = i;
                }
                var lc = imagePoints[l];
                // determine top, right, bottom midpoints
                double[] angles = new double[4];
                t = (l + 1) % 4;
                b = (l + 1) % 4;
                for (int i = 0; i < 4; i++) {
                    if (i == l) continue;
                    var ic = imagePoints[i];
                    angles[i] = Math.atan2(lc.y - ic.y, ic.x - lc.x);
                    if (angles[i] >= angles[t]) t = i;
                    if (angles[i] <= angles[b]) b = i;
                }
                for (int i = 0; i < 4; i++) {
                    if (i != t && i != l && i != b) r = i;
                }
                // create RotatedRect from midpoints
                var rect =
                        new RotatedRect(
                                new Point(center.x, center.y),
                                new Size(imagePoints[r].x - lc.x, imagePoints[b].y - imagePoints[t].y),
                                Math.toDegrees(-angles[r]));
                // set target corners to rect corners
                Point[] points = new Point[4];
                rect.points(points);
                imagePoints = points;
            }
            // save visible targets for raw video stream simulation
            visibleTgts.add(new Pair<>(tgt, imagePoints));
            // estimate pixel noise
            var noisyTargetCorners = prop.estPixelNoise(imagePoints);
            // find the minimum area rectangle of target corners
            var minAreaRect = OpenCVHelp.getMinAreaRect(noisyTargetCorners);
            Point[] minAreaRectPts = new Point[4];
            minAreaRect.points(minAreaRectPts);
            // find the (naive) 2d yaw/pitch
            var centerPt = minAreaRect.center;
            var centerRot = prop.getPixelRot(centerPt);
            // find contour area
            double areaPercent = prop.getContourAreaPercent(noisyTargetCorners);

            // projected target can't be detected, skip to next
            if (!(canSeeCorners(noisyTargetCorners) && areaPercent >= minTargetAreaPercent)) continue;

            var pnpSim = new PnpResult();
            if (tgt.fiducialID >= 0 && tgt.getFieldVertices().size() == 4) { // single AprilTag solvePNP
                pnpSim =
                        OpenCVHelp.solvePNP_SQPNP(
                                        prop.getIntrinsics(),
                                        prop.getDistCoeffs(),
                                        tgt.getModel().vertices,
                                        noisyTargetCorners)
                                .get();
            }

            detectableTgts.add(
                    new PhotonTrackedTarget(
                            -Math.toDegrees(centerRot.getZ()),
                            -Math.toDegrees(centerRot.getY()),
                            areaPercent,
                            Math.toDegrees(centerRot.getX()),
                            tgt.fiducialID,
                            -1,
                            -1,
                            pnpSim.best,
                            pnpSim.alt,
                            pnpSim.ambiguity,
                            OpenCVHelp.pointsToCorners(minAreaRectPts),
                            OpenCVHelp.pointsToCorners(noisyTargetCorners)));
        }
        // render visible tags to raw video frame
        if (videoSimRawEnabled) {
            // draw field wireframe
            if (videoSimWireframeEnabled) {
                VideoSimUtil.drawFieldWireframe(
                        camRt,
                        prop,
                        videoSimWireframeResolution,
                        1.5,
                        new Scalar(80),
                        6,
                        1,
                        new Scalar(30),
                        videoSimFrameRaw);
            }

            // draw targets
            for (var pair : visibleTgts) {
                var tgt = pair.getFirst();
                var corn = pair.getSecond();

                if (tgt.fiducialID >= 0) { // apriltags
                    VideoSimUtil.warp36h11TagImage(tgt.fiducialID, corn, true, videoSimFrameRaw);
                } else if (!tgt.getModel().isSpherical) { // non-spherical targets
                    var contour = corn;
                    if (!tgt.getModel()
                            .isPlanar) { // visualization cant handle non-convex projections of 3d models
                        contour = OpenCVHelp.getConvexHull(contour);
                    }
                    VideoSimUtil.drawPoly(contour, -1, new Scalar(255), true, videoSimFrameRaw);
                } else { // spherical targets
                    VideoSimUtil.drawInscribedEllipse(corn, new Scalar(255), videoSimFrameRaw);
                }
            }
            videoSimRaw.putFrame(videoSimFrameRaw);
        } else videoSimRaw.setConnectionStrategy(ConnectionStrategy.kForceClose);
        // draw/annotate target detection outline on processed view
        if (videoSimProcEnabled) {
            Imgproc.cvtColor(videoSimFrameRaw, videoSimFrameProcessed, Imgproc.COLOR_GRAY2BGR);
            Imgproc.drawMarker( // crosshair
                    videoSimFrameProcessed,
                    new Point(prop.getResWidth() / 2.0, prop.getResHeight() / 2.0),
                    new Scalar(0, 255, 0),
                    Imgproc.MARKER_CROSS,
                    (int) VideoSimUtil.getScaledThickness(15, videoSimFrameProcessed),
                    (int) VideoSimUtil.getScaledThickness(1, videoSimFrameProcessed),
                    Imgproc.LINE_AA);
            for (var tgt : detectableTgts) {
                if (tgt.getFiducialId() >= 0) { // apriltags
                    VideoSimUtil.drawTagDetection(
                            tgt.getFiducialId(),
                            OpenCVHelp.cornersToPoints(tgt.getDetectedCorners()),
                            videoSimFrameProcessed);
                } else { // other targets
                    // bounding rectangle
                    Imgproc.rectangle(
                            videoSimFrameProcessed,
                            OpenCVHelp.getBoundingRect(OpenCVHelp.cornersToPoints(tgt.getDetectedCorners())),
                            new Scalar(0, 0, 255),
                            (int) VideoSimUtil.getScaledThickness(1, videoSimFrameProcessed),
                            Imgproc.LINE_AA);

                    VideoSimUtil.drawPoly(
                            OpenCVHelp.cornersToPoints(tgt.getMinAreaRectCorners()),
                            (int) VideoSimUtil.getScaledThickness(1, videoSimFrameProcessed),
                            new Scalar(255, 30, 30),
                            true,
                            videoSimFrameProcessed);
                }
            }
            videoSimProcessed.putFrame(videoSimFrameProcessed);
        } else videoSimProcessed.setConnectionStrategy(ConnectionStrategy.kForceClose);

        // calculate multitag results
        Optional<MultiTargetPNPResult> multitagResult = Optional.empty();
        // TODO: Implement ATFL subscribing in backend
        // var tagLayout = cam.getAprilTagFieldLayout();
        var visibleLayoutTags = VisionEstimation.getVisibleLayoutTags(detectableTgts, tagLayout);
        if (visibleLayoutTags.size() > 1) {
            List<Short> usedIDs =
                    visibleLayoutTags.stream().map(t -> (short) t.ID).sorted().collect(Collectors.toList());
            var pnpResult =
                    VisionEstimation.estimateCamPosePNP(
                            prop.getIntrinsics(),
                            prop.getDistCoeffs(),
                            detectableTgts,
                            tagLayout,
                            TargetModel.kAprilTag36h11);

            if (pnpResult.isPresent()) {
                multitagResult = Optional.of(new MultiTargetPNPResult(pnpResult.get(), usedIDs));
            }
        }

        // sort target order
        if (sortMode != null) {
            detectableTgts.sort(sortMode.getComparator());
        }

        // put this simulated data to NT
        var now = RobotController.getFPGATime();
        var ret =
                new PhotonPipelineResult(
                        heartbeatCounter,
                        now - (long) (latencyMillis * 1000),
                        now,
                        // Pretend like we heard a pong recently
                        1000L + (long) ((Math.random() - 0.5) * 50),
                        detectableTgts,
                        multitagResult);
        return ret;
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
        ts.latencyMillisEntry.set(result.metadata.getLatencyMillis(), receiveTimestamp);

        // Results are now dynamically sized, so let's guess 1024 bytes is big enough
        ts.resultPublisher.set(result, 1024);

        boolean hasTargets = result.hasTargets();
        ts.hasTargetEntry.set(hasTargets, receiveTimestamp);
        if (!hasTargets) {
            ts.targetPitchEntry.set(0.0, receiveTimestamp);
            ts.targetYawEntry.set(0.0, receiveTimestamp);
            ts.targetAreaEntry.set(0.0, receiveTimestamp);
            ts.targetPoseEntry.set(new Transform3d(), receiveTimestamp);
            ts.targetSkewEntry.set(0.0, receiveTimestamp);
        } else {
            var bestTarget = result.getBestTarget();

            ts.targetPitchEntry.set(bestTarget.getPitch(), receiveTimestamp);
            ts.targetYawEntry.set(bestTarget.getYaw(), receiveTimestamp);
            ts.targetAreaEntry.set(bestTarget.getArea(), receiveTimestamp);
            ts.targetSkewEntry.set(bestTarget.getSkew(), receiveTimestamp);

            var transform = bestTarget.getBestCameraToTarget();
            ts.targetPoseEntry.set(transform, receiveTimestamp);
        }

        ts.cameraIntrinsicsPublisher.set(prop.getIntrinsics().getData(), receiveTimestamp);
        ts.cameraDistortionPublisher.set(prop.getDistCoeffs().getData(), receiveTimestamp);

        ts.heartbeatPublisher.set(heartbeatCounter, receiveTimestamp);
        heartbeatCounter += 1;
    }
}
