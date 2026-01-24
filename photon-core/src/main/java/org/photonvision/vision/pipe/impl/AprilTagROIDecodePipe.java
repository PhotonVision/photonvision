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

package org.photonvision.vision.pipe.impl;

import edu.wpi.first.apriltag.AprilTagDetection;
import edu.wpi.first.apriltag.AprilTagDetector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.apriltag.AprilTagFamily;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.opencv.Releasable;
import org.photonvision.vision.pipe.CVPipe;

/**
 * Pipe that decodes AprilTags within ROIs detected by ML. For each ROI from ML detection, it
 * extracts a sub-image, runs the traditional WPILib AprilTag detector, and maps the detected corner
 * coordinates and homography back to full-frame coordinates.
 *
 * <p>Both corner coordinates AND the homography matrix must be transformed from ROI
 * coordinates to full-frame coordinates.
 */
public class AprilTagROIDecodePipe
        extends CVPipe<
                AprilTagROIDecodePipe.ROIDecodeInput,
                List<AprilTagDetection>,
                AprilTagROIDecodePipe.ROIDecodeParams>
        implements Releasable {

    private static final Logger logger = new Logger(AprilTagROIDecodePipe.class, LogGroup.VisionModule);
    private static final boolean DEBUG_COORDINATE_MAPPING = false;

    /** Termination criteria for cornerSubPix refinement */
    private final TermCriteria cornerSubPixCriteria =
            new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 30, 0.001);

    /**
     * Context object containing ATR scaling information for a single ROI.
     */
    private static class ATRContext {
        final double scaleFactor;
        final boolean wasScaled;

        ATRContext(double scaleFactor) {
            this.scaleFactor = scaleFactor;
            this.wasScaled = scaleFactor < 1.0;
        }

        static ATRContext noScaling() {
            return new ATRContext(1.0);
        }
    }

    /** Input container for ROI decode pipe */
    public static class ROIDecodeInput {
        public final CVMat grayFrame;
        public final List<Rect2d> rois;

        public ROIDecodeInput(CVMat grayFrame, List<Rect2d> rois) {
            this.grayFrame = grayFrame;
            this.rois = rois;
        }
    }

    /** Parameters for ROI decode pipe */
    public static class ROIDecodeParams {
        public AprilTagFamily tagFamily = AprilTagFamily.kTag36h11;
        public double roiExpansionFactor = 1.2;
        public AprilTagDetector.Config detectorConfig;
        public AprilTagDetector.QuadThresholdParameters quadParams;
        public int maxHammingDistance = 0;
        public double minDecisionMargin = 35;

        // Adaptive Tag Resizing (ATR) parameters
        /** Enable ATR (adaptive tag resizing) */
        public boolean atrEnabled = true;
        /** Target dimension for ATR - tags wider than this will be downscaled */
        public int atrTargetDimension = 160;
        /** Minimum scale factor for ATR (prevents extreme downscaling) */
        public double atrMinScaleFactor = 0.25;
        /** Enable corner refinement (two-stage coarse-to-fine) */
        public boolean atrCornerRefinementEnabled = false;
        /** Window size for corner refinement */
        public int atrRefinementWindowSize = 5;

        public ROIDecodeParams() {
            detectorConfig = new AprilTagDetector.Config();
            detectorConfig.numThreads = 1;
            detectorConfig.quadDecimate = 1; 
            quadParams = new AprilTagDetector.QuadThresholdParameters();
            // Match the defaults from AprilTagPipeline
            quadParams.minClusterPixels = 5;
            quadParams.maxNumMaxima = 10;
            quadParams.criticalAngle = 45 * Math.PI / 180.0;
            quadParams.maxLineFitMSE = 10.0f;
            quadParams.minWhiteBlackDiff = 5;
            quadParams.deglitch = false;
        }
    }

    private AprilTagDetector detector;
    private AprilTagFamily currentFamily;

    public AprilTagROIDecodePipe() {
        detector = new AprilTagDetector();
    }

    @Override
    protected List<AprilTagDetection> process(ROIDecodeInput input) {
        List<AprilTagDetection> allDetections = new ArrayList<>();

        if (input.grayFrame == null || input.grayFrame.getMat().empty()) {
            return allDetections;
        }

        Mat fullFrame = input.grayFrame.getMat();
        int frameWidth = fullFrame.cols();
        int frameHeight = fullFrame.rows();

        for (Rect2d roi : input.rois) {
            // Expand the ROI and track the EXPANDED coordinates for mapping
            Rect2d expandedROI = expandBbox(roi, params.roiExpansionFactor, frameWidth, frameHeight);
            Rect roiRect = toIntRect(expandedROI);

            if (roiRect.width <= 0 || roiRect.height <= 0) {
                continue;
            }

            // Clamp to frame bounds (defensive)
            roiRect = clampToFrame(roiRect, frameWidth, frameHeight);
            if (roiRect.width <= 0 || roiRect.height <= 0) {
                continue;
            }

            if (DEBUG_COORDINATE_MAPPING) {
                logger.debug(
                        "Original ROI: x="
                                + roi.x
                                + ", y="
                                + roi.y
                                + ", w="
                                + roi.width
                                + ", h="
                                + roi.height);
                logger.debug(
                        "Expanded ROI (used for mapping): x="
                                + roiRect.x
                                + ", y="
                                + roiRect.y
                                + ", w="
                                + roiRect.width
                                + ", h="
                                + roiRect.height);
            }

            // Extract full-resolution submat
            Mat roiMat = fullFrame.submat(roiRect);

            // === STAGE 1: COARSE - Calculate scaling and resize ===
            ATRContext atrContext;
            Mat processingMat;

            if (params.atrEnabled && roiRect.width > params.atrTargetDimension) {
                // Calculate scale factor: S = min(1.0, T_dim / w)
                double S = (double) params.atrTargetDimension / roiRect.width;
                S = Math.max(S, params.atrMinScaleFactor); // Clamp to prevent extreme scaling

                int scaledWidth = (int) Math.round(roiRect.width * S);
                int scaledHeight = (int) Math.round(roiRect.height * S);

                atrContext = new ATRContext(S);

                // Downscale ROI to target dimension
                processingMat = new Mat();
                Imgproc.resize(
                        roiMat,
                        processingMat,
                        new Size(scaledWidth, scaledHeight),
                        0,
                        0,
                        Imgproc.INTER_AREA); // INTER_AREA best for downscaling

                if (DEBUG_COORDINATE_MAPPING) {
                    logger.debug(
                            "ATR: ROI "
                                    + roiRect.width
                                    + "x"
                                    + roiRect.height
                                    + " -> "
                                    + scaledWidth
                                    + "x"
                                    + scaledHeight
                                    + " (S="
                                    + S
                                    + ")");
                }
            } else {
                // No scaling needed (tag already small or ATR disabled)
                atrContext = ATRContext.noScaling();
                processingMat = roiMat;
            }

            // Run AprilTag detection on (possibly scaled) image
            AprilTagDetection[] roiDetections = detector.detect(processingMat);

            for (AprilTagDetection det : roiDetections) {
                if (det.getHamming() > params.maxHammingDistance) continue;
                if (det.getDecisionMargin() < params.minDecisionMargin) continue;

                // === STAGE 2: PROJECTION - Map coordinates back to full-frame ===
                AprilTagDetection mappedDetection;
                if (atrContext.wasScaled) {
                    mappedDetection = mapToFullFrameWithATR(det, roiRect, atrContext);
                } else {
                    mappedDetection = mapToFullFrame(det, roiRect);
                }

                // === STAGE 3: FINE REFINEMENT - Sub-pixel corner accuracy ===
                if (params.atrCornerRefinementEnabled && atrContext.wasScaled) {
                    mappedDetection = refineCorners(mappedDetection, fullFrame);
                }

                if (DEBUG_COORDINATE_MAPPING) {
                    logger.debug(
                            "Tag "
                                    + det.getId()
                                    + " corner 0: ROI=("
                                    + det.getCornerX(0)
                                    + ", "
                                    + det.getCornerY(0)
                                    + "), Full=("
                                    + mappedDetection.getCornerX(0)
                                    + ", "
                                    + mappedDetection.getCornerY(0)
                                    + ")");
                }

                allDetections.add(mappedDetection);
            }

            // Cleanup
            roiMat.release();
            if (atrContext.wasScaled) {
                processingMat.release();
            }
        }

        // === STAGE 4: POSE ESTIMATION happens in AprilTagPoseEstimatorPipe ===
        return deduplicateByTagId(allDetections);
    }

    /**
     * Maps detection coordinates from ROI space to full-frame space.
     *
     * Both corners AND homography must be transformed. The pose estimator uses the
     * homography matrix internally for pose estimation, not just the corners.
     *
     * @param det The detection in ROI coordinates
     * @param roiOffset The ROI rectangle defining the offset from full frame origin
     * @return A new AprilTagDetection with coordinates mapped to full frame
     */
    private AprilTagDetection mapToFullFrame(AprilTagDetection det, Rect roiOffset) {
        // Map all 4 corners from ROI coordinates to full-frame coordinates
        double[] mappedCorners = new double[8];
        for (int i = 0; i < 4; i++) {
            mappedCorners[i * 2] = det.getCornerX(i) + roiOffset.x;
            mappedCorners[i * 2 + 1] = det.getCornerY(i) + roiOffset.y;
        }

        // Map center coordinate
        double centerX = det.getCenterX() + roiOffset.x;
        double centerY = det.getCenterY() + roiOffset.y;

        // Transform homography from ROI coordinates to full-frame coordinates
        double[] transformedHomography =
                transformHomography(det.getHomography(), roiOffset.x, roiOffset.y);

        return new AprilTagDetection(
                det.getFamily(),
                det.getId(),
                det.getHamming(),
                det.getDecisionMargin(),
                transformedHomography,
                centerX,
                centerY,
                mappedCorners);
    }

    /**
     * Maps detection coordinates from scaled ROI space to full-frame space.
     * Handles both translation AND scaling transformations.
     *
     * @param det The detection in scaled ROI coordinates
     * @param roiOffset The ROI rectangle defining the offset from full frame origin
     * @param atrContext The ATR scaling context
     * @return A new AprilTagDetection with coordinates mapped to full frame
     */
    private AprilTagDetection mapToFullFrameWithATR(
            AprilTagDetection det, Rect roiOffset, ATRContext atrContext) {
        double invS = 1.0 / atrContext.scaleFactor;

        // Map all 4 corners from scaled ROI coordinates to full-frame coordinates
        // x_full = (x_scaled / S) + roi_x
        double[] mappedCorners = new double[8];
        for (int i = 0; i < 4; i++) {
            mappedCorners[i * 2] = det.getCornerX(i) * invS + roiOffset.x;
            mappedCorners[i * 2 + 1] = det.getCornerY(i) * invS + roiOffset.y;
        }

        // Map center coordinate
        double centerX = det.getCenterX() * invS + roiOffset.x;
        double centerY = det.getCenterY() * invS + roiOffset.y;

        // Transform homography with scaling
        double[] transformedHomography =
                transformHomographyWithScale(
                        det.getHomography(), roiOffset.x, roiOffset.y, atrContext.scaleFactor);

        return new AprilTagDetection(
                det.getFamily(),
                det.getId(),
                det.getHamming(),
                det.getDecisionMargin(),
                transformedHomography,
                centerX,
                centerY,
                mappedCorners);
    }

    /**
     * Transforms a homography matrix from ROI coordinates to full-frame coordinates.
     *
     * <p>The homography H satisfies: [x_roi, y_roi, 1]^T ~ H * [X_tag, Y_tag, 1]^T
     *
     * <p>To convert to full-frame coordinates where x_full = x_roi + offsetX and
     * y_full = y_roi + offsetY, we compute H_full = T * H_roi where T is the translation matrix:
     *
     * <pre>
     * | 1  0  offsetX |
     * | 0  1  offsetY |
     * | 0  0  1       |
     * </pre>
     *
     * <p>The UMich AprilTag library stores homography as row-major 3x3:
     * [h00, h01, h02, h10, h11, h12, h20, h21, h22]
     * source: https://april.eecs.umich.edu/media/pdfs/olson2011tags.pdf
     *
     * @param h The original homography (9 elements, row-major 3x3)
     * @param offsetX The x offset from ROI origin to full-frame origin
     * @param offsetY The y offset from ROI origin to full-frame origin
     * @return The transformed homography in full-frame coordinates
     */
    private double[] transformHomography(double[] h, int offsetX, int offsetY) {
        // T * H where T = [[1,0,tx],[0,1,ty],[0,0,1]]
        // (T*H)[0][j] = 1*H[0][j] + 0*H[1][j] + tx*H[2][j] = H[0][j] + tx*H[2][j]
        // (T*H)[1][j] = 0*H[0][j] + 1*H[1][j] + ty*H[2][j] = H[1][j] + ty*H[2][j]
        // (T*H)[2][j] = 0*H[0][j] + 0*H[1][j] + 1*H[2][j] = H[2][j]

        double[] result = new double[9];
        // Row 0: H[0][j] + offsetX * H[2][j]
        result[0] = h[0] + offsetX * h[6];
        result[1] = h[1] + offsetX * h[7];
        result[2] = h[2] + offsetX * h[8];
        // Row 1: H[1][j] + offsetY * H[2][j]
        result[3] = h[3] + offsetY * h[6];
        result[4] = h[4] + offsetY * h[7];
        result[5] = h[5] + offsetY * h[8];
        // Row 2: unchanged
        result[6] = h[6];
        result[7] = h[7];
        result[8] = h[8];
        return result;
    }

    /**
     * Refines corner positions using cornerSubPix on the full-resolution image.
     * This is the "fine" stage of coarse-to-fine detection.
     *
     * <p>CRITICAL: The homography MUST be recomputed from refined corners because
     * the pose estimator uses BOTH corners and homography internally. If they
     * become inconsistent (corners change but homography doesn't), pose will be wrong.
     *
     * @param detection The detection with corners mapped to full-frame coordinates
     * @param fullFrame The full grayscale frame for sub-pixel refinement
     * @return A new AprilTagDetection with refined corners and recomputed homography
     */
    private AprilTagDetection refineCorners(AprilTagDetection detection, Mat fullFrame) {
        // Extract corners into MatOfPoint2f
        Point[] corners = new Point[4];
        for (int i = 0; i < 4; i++) {
            corners[i] = new Point(detection.getCornerX(i), detection.getCornerY(i));
        }
        MatOfPoint2f cornerMat = new MatOfPoint2f(corners);

        // Window size for cornerSubPix
        Size winSize = new Size(params.atrRefinementWindowSize, params.atrRefinementWindowSize);
        Size zeroZone = new Size(-1, -1); // No dead zone

        try {
            // Run cornerSubPix refinement on full-resolution image
            Imgproc.cornerSubPix(fullFrame, cornerMat, winSize, zeroZone, cornerSubPixCriteria);

            // Extract refined corners
            Point[] refinedCorners = cornerMat.toArray();
            double[] refinedCornersArr = new double[8];
            for (int i = 0; i < 4; i++) {
                refinedCornersArr[i * 2] = refinedCorners[i].x;
                refinedCornersArr[i * 2 + 1] = refinedCorners[i].y;
            }

            // Compute refined center
            double centerX =
                    (refinedCorners[0].x
                                    + refinedCorners[1].x
                                    + refinedCorners[2].x
                                    + refinedCorners[3].x)
                            / 4.0;
            double centerY =
                    (refinedCorners[0].y
                                    + refinedCorners[1].y
                                    + refinedCorners[2].y
                                    + refinedCorners[3].y)
                            / 4.0;

            // CRITICAL: Recompute homography from refined corners
            double[] refinedHomography = computeHomographyFromCorners(refinedCorners);

            return new AprilTagDetection(
                    detection.getFamily(),
                    detection.getId(),
                    detection.getHamming(),
                    detection.getDecisionMargin(),
                    refinedHomography,
                    centerX,
                    centerY,
                    refinedCornersArr);
        } finally {
            cornerMat.release();
        }
    }

    /**
     * Computes the homography matrix from 4 corner points.
     *
     * <p>The AprilTag library uses a Y-down coordinate system for its homography,
     * matching image coordinates. The normalized tag coordinates are:
     * <ul>
     *   <li>Corner 0: (-1,  1) - bottom-left (larger Y in image space)</li>
     *   <li>Corner 1: ( 1,  1) - bottom-right</li>
     *   <li>Corner 2: ( 1, -1) - top-right (smaller Y in image space)</li>
     *   <li>Corner 3: (-1, -1) - top-left</li>
     * </ul>
     *
     * <p>The homography H satisfies: [x_img, y_img, 1]^T ~ H * [X_tag, Y_tag, 1]^T
     *
     * @param corners The 4 corner points in image coordinates
     * @return The homography matrix as a 9-element row-major array
     */
    private double[] computeHomographyFromCorners(Point[] corners) {
        // Normalized tag corner coordinates (AprilTag convention with Y-down)
        // Corner 0: bottom-left (larger Y in image space)
        // Corner 1: bottom-right
        // Corner 2: top-right (smaller Y in image space)
        // Corner 3: top-left
        MatOfPoint2f srcPoints =
                new MatOfPoint2f(
                        new Point(-1, 1), // corner 0: bottom-left
                        new Point(1, 1), // corner 1: bottom-right
                        new Point(1, -1), // corner 2: top-right
                        new Point(-1, -1) // corner 3: top-left
                        );

        MatOfPoint2f dstPoints = new MatOfPoint2f(corners);

        // Compute homography: srcPoints -> dstPoints
        Mat H = Calib3d.findHomography(srcPoints, dstPoints);

        // Extract to row-major double array (AprilTag format)
        double[] homography = new double[9];
        H.get(0, 0, homography);

        // Cleanup
        srcPoints.release();
        dstPoints.release();
        H.release();

        return homography;
    }

    /**
     * Transforms a homography matrix from scaled ROI coordinates to full-frame coordinates.
     *
     * <p>The transformation combines inverse scaling and translation:
     * H_full = T * S_inv * H_scaled
     *
     * <p>Where:
     * - S_inv = [[1/S, 0, 0], [0, 1/S, 0], [0, 0, 1]] (inverse scale)
     * - T = [[1, 0, offsetX], [0, 1, offsetY], [0, 0, 1]] (translation)
     *
     * <p>Combined formula:
     * Row 0: H[0..2] / S + offsetX * H[6..8]
     * Row 1: H[3..5] / S + offsetY * H[6..8]
     * Row 2: unchanged (H[6..8])
     *
     * @param h The original homography (9 elements, row-major 3x3)
     * @param offsetX The x offset from ROI origin to full-frame origin
     * @param offsetY The y offset from ROI origin to full-frame origin
     * @param S The scale factor used for ROI resizing (< 1 means downscaled)
     * @return The transformed homography in full-frame coordinates
     */
    private double[] transformHomographyWithScale(double[] h, int offsetX, int offsetY, double S) {
        double invS = 1.0 / S;
        double[] result = new double[9];

        // Row 0: H[i]/S + offsetX * H[6+j]
        result[0] = h[0] * invS + offsetX * h[6];
        result[1] = h[1] * invS + offsetX * h[7];
        result[2] = h[2] * invS + offsetX * h[8];

        // Row 1: H[i]/S + offsetY * H[6+j]
        result[3] = h[3] * invS + offsetY * h[6];
        result[4] = h[4] * invS + offsetY * h[7];
        result[5] = h[5] * invS + offsetY * h[8];

        // Row 2: unchanged
        result[6] = h[6];
        result[7] = h[7];
        result[8] = h[8];

        return result;
    }

    /**
     * Expands a bounding box by a scale factor while clamping to image bounds.
     *
     * @param bbox Original bounding box
     * @param scale Expansion scale factor (1.0 = no change, 1.2 = 20% larger)
     * @param imageWidth Image width for clamping
     * @param imageHeight Image height for clamping
     * @return Expanded and clamped bounding box
     */
    private Rect2d expandBbox(Rect2d bbox, double scale, int imageWidth, int imageHeight) {
        double newWidth = bbox.width * scale;
        double newHeight = bbox.height * scale;
        double newX = bbox.x - (newWidth - bbox.width) / 2.0;
        double newY = bbox.y - (newHeight - bbox.height) / 2.0;

        // Clamp to image bounds
        newX = Math.max(0, newX);
        newY = Math.max(0, newY);
        newWidth = Math.min(newWidth, imageWidth - newX);
        newHeight = Math.min(newHeight, imageHeight - newY);

        return new Rect2d(newX, newY, newWidth, newHeight);
    }

    /**
     * Converts a Rect2d to integer Rect using floor for position and ceil for size to ensure we
     * don't clip the tag area.
     */
    private Rect toIntRect(Rect2d r) {
        return new Rect(
                (int) Math.floor(r.x),
                (int) Math.floor(r.y),
                (int) Math.ceil(r.width),
                (int) Math.ceil(r.height));
    }

    /** Clamps a rectangle to frame bounds. */
    private Rect clampToFrame(Rect r, int frameWidth, int frameHeight) {
        int x = Math.max(0, r.x);
        int y = Math.max(0, r.y);
        int w = Math.min(r.width, frameWidth - x);
        int h = Math.min(r.height, frameHeight - y);
        return new Rect(x, y, w, h);
    }

    /**
     * Deduplicates detections by tag ID, keeping the one with highest decision margin. This handles
     * cases where overlapping ROIs detect the same tag.
     */
    private List<AprilTagDetection> deduplicateByTagId(List<AprilTagDetection> detections) {
        Map<Integer, AprilTagDetection> bestByTagId = new HashMap<>();

        for (AprilTagDetection det : detections) {
            int tagId = det.getId();
            AprilTagDetection existing = bestByTagId.get(tagId);

            if (existing == null || det.getDecisionMargin() > existing.getDecisionMargin()) {
                bestByTagId.put(tagId, det);
            }
        }

        return new ArrayList<>(bestByTagId.values());
    }

    @Override
    public void setParams(ROIDecodeParams newParams) {
        if (newParams.tagFamily != currentFamily) {
            detector.clearFamilies();
            detector.addFamily(newParams.tagFamily.getNativeName());
            currentFamily = newParams.tagFamily;
        }

        detector.setConfig(newParams.detectorConfig);
        detector.setQuadThresholdParameters(newParams.quadParams);

        super.setParams(newParams);
    }

    @Override
    public void release() {
        if (detector != null) {
            detector.close();
            detector = null;
        }
    }
}
