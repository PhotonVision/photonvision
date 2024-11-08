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
import edu.wpi.first.cscore.CvSource;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.util.RawFrame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.photonvision.estimation.OpenCVHelp;
import org.photonvision.estimation.RotTrlTransform3d;

public class VideoSimUtil {
    public static final int kNumTags36h11 = 30;

    // All 36h11 tag images
    private static final Map<Integer, Mat> kTag36h11Images = new HashMap<>();
    // Points corresponding to marker(black square) corners of 10x10 36h11 tag images
    public static final Point[] kTag36h11MarkerPts;

    // field dimensions for wireframe
    private static double fieldLength = 16.54175;
    private static double fieldWidth = 8.0137;

    static {
        OpenCVHelp.forceLoadOpenCV();

        // create Mats of 10x10 apriltag images
        for (int i = 0; i < VideoSimUtil.kNumTags36h11; i++) {
            Mat tagImage = VideoSimUtil.get36h11TagImage(i);
            kTag36h11Images.put(i, tagImage);
        }

        kTag36h11MarkerPts = get36h11MarkerPts();
    }

    /** Updates the properties of this CvSource video stream with the given camera properties. */
    public static void updateVideoProp(CvSource video, SimCameraProperties prop) {
        video.setResolution(prop.getResWidth(), prop.getResHeight());
        video.setFPS((int) prop.getFPS());
    }

    /**
     * Gets the points representing the corners of this image. Because image pixels are accessed
     * through a Mat, the point (0,0) actually represents the center of the top-left pixel and not the
     * actual top-left corner.
     *
     * @param size Size of image
     */
    public static Point[] getImageCorners(Size size) {
        return new Point[] {
            new Point(-0.5, -0.5),
            new Point(size.width - 0.5, -0.5),
            new Point(size.width - 0.5, size.height - 0.5),
            new Point(-0.5, size.height - 0.5)
        };
    }

    /**
     * Gets the 10x10 (grayscale) image of a specific 36h11 AprilTag.
     *
     * @param id The fiducial id of the desired tag
     */
    private static Mat get36h11TagImage(int id) {
        RawFrame frame = AprilTag.generate36h11AprilTagImage(id);

        var buf = frame.getData();
        byte[] arr = new byte[buf.remaining()];
        buf.get(arr);
        // frame.close();

        var mat = new MatOfByte(arr).reshape(1, 10).submat(new Rect(0, 0, 10, 10));
        mat.dump();

        return mat;
    }

    /** Gets the points representing the marker(black square) corners. */
    public static Point[] get36h11MarkerPts() {
        return get36h11MarkerPts(1);
    }

    /**
     * Gets the points representing the marker(black square) corners.
     *
     * @param scale The scale of the tag image (10*scale x 10*scale image)
     */
    public static Point[] get36h11MarkerPts(int scale) {
        var roi36h11 = new Rect(new Point(1, 1), new Size(8, 8));
        roi36h11.x *= scale;
        roi36h11.y *= scale;
        roi36h11.width *= scale;
        roi36h11.height *= scale;
        var pts = getImageCorners(roi36h11.size());
        for (int i = 0; i < pts.length; i++) {
            var pt = pts[i];
            pts[i] = new Point(roi36h11.tl().x + pt.x, roi36h11.tl().y + pt.y);
        }
        return pts;
    }

    /**
     * Warps the image of a specific 36h11 AprilTag onto the destination image at the given points.
     *
     * @param tagId The id of the specific tag to warp onto the destination image
     * @param dstPoints Points(4) in destination image where the tag marker(black square) corners
     *     should be warped onto.
     * @param antialiasing If antialiasing should be performed by automatically
     *     supersampling/interpolating the warped image. This should be used if better stream quality
     *     is desired or target detection is being done on the stream, but can hurt performance.
     * @param destination The destination image to place the warped tag image onto.
     */
    public static void warp36h11TagImage(
            int tagId, Point[] dstPoints, boolean antialiasing, Mat destination) {
        Mat tagImage = kTag36h11Images.get(tagId);
        if (tagImage == null || tagImage.empty()) return;
        var tagPoints = new MatOfPoint2f(kTag36h11MarkerPts);
        // points of tag image corners
        var tagImageCorners = new MatOfPoint2f(getImageCorners(tagImage.size()));
        var dstPointMat = new MatOfPoint2f(dstPoints);
        // the rectangle describing the rectangle-of-interest(ROI)
        var boundingRect = Imgproc.boundingRect(dstPointMat);
        // find the perspective transform from the tag image to the warped destination points
        Mat perspecTrf = Imgproc.getPerspectiveTransform(tagPoints, dstPointMat);
        // check extreme image corners after transform to check if we need to expand bounding rect
        var extremeCorners = new MatOfPoint2f();
        Core.perspectiveTransform(tagImageCorners, extremeCorners, perspecTrf);
        // dilate ROI to fit full tag
        boundingRect = Imgproc.boundingRect(extremeCorners);

        // adjust interpolation strategy based on size of warped tag compared to tag image
        var warpedContourArea = Imgproc.contourArea(extremeCorners);
        double warpedTagUpscale = Math.sqrt(warpedContourArea) / Math.sqrt(tagImage.size().area());
        int warpStrategy = Imgproc.INTER_NEAREST;
        // automatically determine the best supersampling of warped image and scale of tag image
        /*
        (warpPerspective does not properly resample, so this is used to avoid aliasing in the
        warped image. Supersampling is used when the warped tag is small, but is very slow
        when the warped tag is large-- scaling the tag image up and using linear interpolation
        instead can be performant while still effectively antialiasing. Some combination of these
        two can be used in between those extremes.)

        TODO: Simplify magic numbers to one or two variables, or use a more proper approach?
        */
        int supersampling = 6;
        supersampling = (int) Math.ceil(supersampling / warpedTagUpscale);
        supersampling = Math.max(Math.min(supersampling, 10), 1);

        Mat scaledTagImage = new Mat();
        if (warpedTagUpscale > 2.0) {
            warpStrategy = Imgproc.INTER_LINEAR;
            int scaleFactor = (int) (warpedTagUpscale / 3.0) + 2;
            scaleFactor = Math.max(Math.min(scaleFactor, 40), 1);
            scaleFactor *= supersampling;
            Imgproc.resize(
                    tagImage, scaledTagImage, new Size(), scaleFactor, scaleFactor, Imgproc.INTER_NEAREST);
            tagPoints.fromArray(get36h11MarkerPts(scaleFactor));
        } else tagImage.assignTo(scaledTagImage);

        // constrain the bounding rect inside of the destination image
        boundingRect.x -= 1;
        boundingRect.y -= 1;
        boundingRect.width += 2;
        boundingRect.height += 2;
        if (boundingRect.x < 0) {
            boundingRect.width += boundingRect.x;
            boundingRect.x = 0;
        }
        if (boundingRect.y < 0) {
            boundingRect.height += boundingRect.y;
            boundingRect.y = 0;
        }
        boundingRect.width = Math.min(destination.width() - boundingRect.x, boundingRect.width);
        boundingRect.height = Math.min(destination.height() - boundingRect.y, boundingRect.height);
        if (boundingRect.width <= 0 || boundingRect.height <= 0) return;

        // upscale if supersampling
        Mat scaledDstPts = new Mat();
        if (supersampling > 1) {
            Core.multiply(dstPointMat, new Scalar(supersampling, supersampling), scaledDstPts);
            boundingRect.x *= supersampling;
            boundingRect.y *= supersampling;
            boundingRect.width *= supersampling;
            boundingRect.height *= supersampling;
        } else dstPointMat.assignTo(scaledDstPts);

        // update transform relative to expanded, scaled bounding rect
        Core.subtract(scaledDstPts, new Scalar(boundingRect.tl().x, boundingRect.tl().y), scaledDstPts);
        perspecTrf = Imgproc.getPerspectiveTransform(tagPoints, scaledDstPts);

        // warp (scaled) tag image onto (scaled) ROI image representing the portion of
        // the destination image encapsulated by boundingRect
        Mat tempROI = new Mat();
        Imgproc.warpPerspective(scaledTagImage, tempROI, perspecTrf, boundingRect.size(), warpStrategy);

        // downscale ROI with interpolation if supersampling
        if (supersampling > 1) {
            boundingRect.x /= supersampling;
            boundingRect.y /= supersampling;
            boundingRect.width /= supersampling;
            boundingRect.height /= supersampling;
            Imgproc.resize(tempROI, tempROI, boundingRect.size(), 0, 0, Imgproc.INTER_AREA);
        }

        // we want to copy ONLY the transformed tag to the result image, not the entire bounding rect
        // using a mask only copies the source pixels which have an associated non-zero value in the
        // mask
        Mat tempMask = Mat.zeros(tempROI.size(), CvType.CV_8UC1);
        Core.subtract(
                extremeCorners, new Scalar(boundingRect.tl().x, boundingRect.tl().y), extremeCorners);
        Point tempCenter = new Point();
        tempCenter.x =
                Arrays.stream(extremeCorners.toArray()).mapToDouble(p -> p.x).average().getAsDouble();
        tempCenter.y =
                Arrays.stream(extremeCorners.toArray()).mapToDouble(p -> p.y).average().getAsDouble();
        // dilate tag corners
        Arrays.stream(extremeCorners.toArray())
                .forEach(
                        p -> {
                            double xdiff = p.x - tempCenter.x;
                            double ydiff = p.y - tempCenter.y;
                            xdiff += 1 * Math.signum(xdiff);
                            ydiff += 1 * Math.signum(ydiff);
                            new Point(tempCenter.x + xdiff, tempCenter.y + ydiff);
                        });
        // (make inside of tag completely white in mask)
        Imgproc.fillConvexPoly(tempMask, new MatOfPoint(extremeCorners.toArray()), new Scalar(255));

        // copy transformed tag onto result image
        tempROI.copyTo(destination.submat(boundingRect), tempMask);
    }

    /**
     * Given a line thickness in a 640x480 image, try to scale to the given destination image
     * resolution.
     *
     * @param thickness480p A hypothetical line thickness in a 640x480 image
     * @param destinationImg The destination image to scale to
     * @return Scaled thickness which cannot be less than 1
     */
    public static double getScaledThickness(double thickness480p, Mat destinationImg) {
        double scaleX = destinationImg.width() / 640.0;
        double scaleY = destinationImg.height() / 480.0;
        double minScale = Math.min(scaleX, scaleY);
        return Math.max(thickness480p * minScale, 1.0);
    }

    /**
     * Draw a filled ellipse in the destination image.
     *
     * @param dstPoints The points in the destination image representing the rectangle in which the
     *     ellipse is inscribed.
     * @param color The color of the ellipse. This is a scalar with BGR values (0-255)
     * @param destination The destination image to draw onto. The image should be in the BGR color
     *     space.
     */
    public static void drawInscribedEllipse(Point[] dstPoints, Scalar color, Mat destination) {
        // create RotatedRect from points
        var rect = OpenCVHelp.getMinAreaRect(dstPoints);
        // inscribe ellipse inside rectangle
        Imgproc.ellipse(destination, rect, color, -1, Imgproc.LINE_AA);
    }

    /**
     * Draw a polygon outline or filled polygon to the destination image with the given points.
     *
     * @param dstPoints The points in the destination image representing the polygon.
     * @param thickness The thickness of the outline in pixels. If this is not positive, a filled
     *     polygon is drawn instead.
     * @param color The color drawn. This should match the color space of the destination image.
     * @param isClosed If the last point should connect to the first point in the polygon outline.
     * @param destination The destination image to draw onto.
     */
    public static void drawPoly(
            Point[] dstPoints, int thickness, Scalar color, boolean isClosed, Mat destination) {
        var dstPointsd = new MatOfPoint(dstPoints);
        if (thickness > 0) {
            Imgproc.polylines(
                    destination, List.of(dstPointsd), isClosed, color, thickness, Imgproc.LINE_AA);
        } else {
            Imgproc.fillPoly(destination, List.of(dstPointsd), color, Imgproc.LINE_AA);
        }
    }

    /**
     * Draws a contour around the given points and text of the id onto the destination image.
     *
     * @param id Fiducial ID number to draw
     * @param dstPoints Points representing the four corners of the tag marker(black square) in the
     *     destination image.
     * @param destination The destination image to draw onto. The image should be in the BGR color
     *     space.
     */
    public static void drawTagDetection(int id, Point[] dstPoints, Mat destination) {
        double thickness = getScaledThickness(1, destination);
        drawPoly(dstPoints, (int) thickness, new Scalar(0, 0, 255), true, destination);
        var rect = Imgproc.boundingRect(new MatOfPoint(dstPoints));
        var textPt = new Point(rect.x + rect.width, rect.y);
        textPt.x += thickness;
        textPt.y += thickness;
        Imgproc.putText(
                destination,
                String.valueOf(id),
                textPt,
                Imgproc.FONT_HERSHEY_PLAIN,
                1.5 * thickness,
                new Scalar(0, 200, 0),
                (int) thickness,
                Imgproc.LINE_AA);
    }

    /**
     * Set the field dimensions that are used for drawing the field wireframe.
     *
     * @param fieldLengthMeters field length in meters (x direction)
     * @param fieldWidthMeters field width in meters (y direction)
     */
    public static void setFieldDimensionsMeters(double fieldLengthMeters, double fieldWidthMeters) {
        fieldLength = fieldLengthMeters;
        fieldWidth = fieldWidthMeters;
    }

    /**
     * The translations used to draw the field side walls and driver station walls. It is a List of
     * Lists because the translations are not all connected.
     */
    private static List<List<Translation3d>> getFieldWallLines() {
        var list = new ArrayList<List<Translation3d>>();

        final double sideHt = Units.inchesToMeters(19.5);
        final double driveHt = Units.inchesToMeters(35);
        final double topHt = Units.inchesToMeters(78);

        // field floor
        list.add(
                List.of(
                        new Translation3d(0, 0, 0),
                        new Translation3d(fieldLength, 0, 0),
                        new Translation3d(fieldLength, fieldWidth, 0),
                        new Translation3d(0, fieldWidth, 0),
                        new Translation3d(0, 0, 0)));
        // right side wall
        list.add(
                List.of(
                        new Translation3d(0, 0, 0),
                        new Translation3d(0, 0, sideHt),
                        new Translation3d(fieldLength, 0, sideHt),
                        new Translation3d(fieldLength, 0, 0)));
        // red driverstation
        list.add(
                List.of(
                        new Translation3d(fieldLength, 0, sideHt),
                        new Translation3d(fieldLength, 0, topHt),
                        new Translation3d(fieldLength, fieldWidth, topHt),
                        new Translation3d(fieldLength, fieldWidth, sideHt)));
        list.add(
                List.of(
                        new Translation3d(fieldLength, 0, driveHt),
                        new Translation3d(fieldLength, fieldWidth, driveHt)));
        // left side wall
        list.add(
                List.of(
                        new Translation3d(0, fieldWidth, 0),
                        new Translation3d(0, fieldWidth, sideHt),
                        new Translation3d(fieldLength, fieldWidth, sideHt),
                        new Translation3d(fieldLength, fieldWidth, 0)));
        // blue driverstation
        list.add(
                List.of(
                        new Translation3d(0, 0, sideHt),
                        new Translation3d(0, 0, topHt),
                        new Translation3d(0, fieldWidth, topHt),
                        new Translation3d(0, fieldWidth, sideHt)));
        list.add(List.of(new Translation3d(0, 0, driveHt), new Translation3d(0, fieldWidth, driveHt)));

        return list;
    }

    /**
     * The translations used to draw the field floor subdivisions (not the floor outline). It is a
     * List of Lists because the translations are not all connected.
     *
     * @param subdivisions How many "subdivisions" along the width/length of the floor. E.g. 3
     *     subdivisions would mean 2 lines along the length and 2 lines along the width creating a 3x3
     *     "grid".
     */
    private static List<List<Translation3d>> getFieldFloorLines(int subdivisions) {
        var list = new ArrayList<List<Translation3d>>();
        final double subLength = fieldLength / subdivisions;
        final double subWidth = fieldWidth / subdivisions;

        // field floor subdivisions
        for (int i = 0; i < subdivisions; i++) {
            list.add(
                    List.of(
                            new Translation3d(0, subWidth * (i + 1), 0),
                            new Translation3d(fieldLength, subWidth * (i + 1), 0)));
            list.add(
                    List.of(
                            new Translation3d(subLength * (i + 1), 0, 0),
                            new Translation3d(subLength * (i + 1), fieldWidth, 0)));
        }

        return list;
    }

    /**
     * Convert 3D lines represented by the given series of translations into a polygon(s) in the
     * camera's image.
     *
     * @param camRt The change in basis from world coordinates to camera coordinates. See {@link
     *     RotTrlTransform3d#makeRelativeTo(Pose3d)}.
     * @param prop The simulated camera's properties.
     * @param trls A sequential series of translations defining the polygon to be drawn.
     * @param resolution Resolution as a fraction(0 - 1) of the video frame's diagonal length in
     *     pixels. Line segments will be subdivided if they exceed this resolution.
     * @param isClosed If the final translation should also draw a line to the first translation.
     * @param destination The destination image that is being drawn to.
     * @return A list of polygons(which are an array of points)
     */
    public static List<Point[]> polyFrom3dLines(
            RotTrlTransform3d camRt,
            SimCameraProperties prop,
            List<Translation3d> trls,
            double resolution,
            boolean isClosed,
            Mat destination) {
        resolution = Math.hypot(destination.size().height, destination.size().width) * resolution;
        List<Translation3d> pts = new ArrayList<>(trls);
        if (isClosed) pts.add(pts.get(0));
        List<Point[]> polyPointList = new ArrayList<>();

        for (int i = 0; i < pts.size() - 1; i++) {
            var pta = pts.get(i);
            var ptb = pts.get(i + 1);

            // check if line is inside camera fulcrum
            var inter = prop.getVisibleLine(camRt, pta, ptb);
            if (inter.getSecond() == null) continue;

            // cull line to the inside of the camera fulcrum
            double inter1 = inter.getFirst();
            double inter2 = inter.getSecond();
            var baseDelta = ptb.minus(pta);
            var old_pta = pta;
            if (inter1 > 0) pta = old_pta.plus(baseDelta.times(inter1));
            if (inter2 < 1) ptb = old_pta.plus(baseDelta.times(inter2));
            baseDelta = ptb.minus(pta);

            // project points into 2d
            var poly =
                    new ArrayList<>(
                            Arrays.asList(
                                    OpenCVHelp.projectPoints(
                                            prop.getIntrinsics(), prop.getDistCoeffs(), camRt, List.of(pta, ptb))));
            var pxa = poly.get(0);
            var pxb = poly.get(1);

            // subdivide projected line based on desired resolution
            double pxDist = Math.hypot(pxb.x - pxa.x, pxb.y - pxa.y);
            int subdivisions = (int) (pxDist / resolution);
            var subDelta = baseDelta.div(subdivisions + 1);
            var subPts = new ArrayList<Translation3d>();
            for (int j = 0; j < subdivisions; j++) {
                subPts.add(pta.plus(subDelta.times(j + 1)));
            }
            if (!subPts.isEmpty()) {
                poly.addAll(
                        1,
                        Arrays.asList(
                                OpenCVHelp.projectPoints(
                                        prop.getIntrinsics(), prop.getDistCoeffs(), camRt, subPts)));
            }

            polyPointList.add(poly.toArray(Point[]::new));
        }

        return polyPointList;
    }

    /**
     * Draw a wireframe of the field to the given image.
     *
     * @param camRt The change in basis from world coordinates to camera coordinates. See {@link
     *     RotTrlTransform3d#makeRelativeTo(Pose3d)}.
     * @param prop The simulated camera's properties.
     * @param resolution Resolution as a fraction(0 - 1) of the video frame's diagonal length in
     *     pixels. Line segments will be subdivided if they exceed this resolution.
     * @param wallThickness Thickness of the lines used for drawing the field walls in pixels. This is
     *     scaled by {@link #getScaledThickness(double, Mat)}.
     * @param wallColor Color of the lines used for drawing the field walls.
     * @param floorSubdivisions A NxN "grid" is created from the floor where this parameter is N,
     *     which defines the floor lines.
     * @param floorThickness Thickness of the lines used for drawing the field floor grid in pixels.
     *     This is scaled by {@link #getScaledThickness(double, Mat)}.
     * @param floorColor Color of the lines used for drawing the field floor grid.
     * @param destination The destination image to draw to.
     */
    public static void drawFieldWireframe(
            RotTrlTransform3d camRt,
            SimCameraProperties prop,
            double resolution,
            double wallThickness,
            Scalar wallColor,
            int floorSubdivisions,
            double floorThickness,
            Scalar floorColor,
            Mat destination) {
        for (var trls : getFieldFloorLines(floorSubdivisions)) {
            var polys = VideoSimUtil.polyFrom3dLines(camRt, prop, trls, resolution, false, destination);
            for (var poly : polys) {
                drawPoly(
                        poly,
                        (int) Math.round(getScaledThickness(floorThickness, destination)),
                        floorColor,
                        false,
                        destination);
            }
        }
        for (var trls : getFieldWallLines()) {
            var polys = VideoSimUtil.polyFrom3dLines(camRt, prop, trls, resolution, false, destination);
            for (var poly : polys) {
                drawPoly(
                        poly,
                        (int) Math.round(getScaledThickness(wallThickness, destination)),
                        wallColor,
                        false,
                        destination);
            }
        }
    }
}
