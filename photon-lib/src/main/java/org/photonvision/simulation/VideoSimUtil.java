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

import edu.wpi.first.cscore.CvSource;
import edu.wpi.first.util.RuntimeLoader;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.photonvision.estimation.OpenCVHelp;

public class VideoSimUtil {
    public static final String kLocalTagImagesPath = "./src/main/resources/images/apriltags/";
    public static final String kResourceTagImagesPath = "/images/apriltags/";
    public static final String kTag16h5ImageName = "tag16_05_00000";
    public static final int kNumTags16h5 = 30;

    // All 16h5 tag images
    private static final Map<Integer, Mat> kTag16h5Images = new HashMap<>();
    // Points corresponding to marker(black square) corners of 8x8 16h5 tag images
    public static final Point[] kTag16h5MarkerPts;

    static {
        try {
            var loader =
                    new RuntimeLoader<>(
                            Core.NATIVE_LIBRARY_NAME, RuntimeLoader.getDefaultExtractionRoot(), Core.class);
            loader.loadLibrary();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load native libraries!", e);
        }

        // create Mats of 8x8 apriltag images
        for (int i = 0; i < VideoSimUtil.kNumTags16h5; i++) {
            Mat tagImage = VideoSimUtil.get16h5TagImage(i);
            kTag16h5Images.put(i, tagImage);
        }

        kTag16h5MarkerPts = get16h5MarkerPts();
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
     * Gets the 8x8 (grayscale) image of a specific 16h5 AprilTag.
     *
     * @param id The fiducial id of the desired tag
     */
    public static Mat get16h5TagImage(int id) {
        String name = kTag16h5ImageName;
        String idString = String.valueOf(id);
        name = name.substring(0, name.length() - idString.length()) + idString;

        var resource = VideoSimUtil.class.getResource(kResourceTagImagesPath + name + ".png");
        // local IDE tests
        String path = kLocalTagImagesPath + name + ".png";
        // gradle tests
        if (resource != null) {
            path = resource.getPath();
            
            // TODO why did we have this previously?
            // if (path.startsWith("/")) path = path.substring(1);
        }
        Mat result = new Mat();
        if (!path.startsWith("file")) result = Imgcodecs.imread(path, Imgcodecs.IMREAD_GRAYSCALE);
        // reading jar file
        if (result.empty()) {
            BufferedImage buf;
            try {
                buf = ImageIO.read(resource);
            } catch (IOException e) {
                System.err.println("Couldn't read tag image!");
                return result;
            }

            result = new Mat(buf.getHeight(), buf.getWidth(), CvType.CV_8UC1);

            byte[] px = new byte[1];
            for (int y = 0; y < result.height(); y++) {
                for (int x = 0; x < result.width(); x++) {
                    px[0] = (byte) (buf.getRGB(x, y) & 0xFF);
                    result.put(y, x, px);
                }
            }
        }
        return result;
    }

    /** Gets the points representing the marker(black square) corners. */
    public static Point[] get16h5MarkerPts() {
        return get16h5MarkerPts(1);
    }

    /**
     * Gets the points representing the marker(black square) corners.
     *
     * @param scale The scale of the tag image (8*scale x 8*scale image)
     */
    public static Point[] get16h5MarkerPts(int scale) {
        var roi16h5 = new Rect(new Point(1, 1), new Size(6, 6));
        roi16h5.x *= scale;
        roi16h5.y *= scale;
        roi16h5.width *= scale;
        roi16h5.height *= scale;
        var pts = getImageCorners(roi16h5.size());
        for (int i = 0; i < pts.length; i++) {
            var pt = pts[i];
            pts[i] = new Point(roi16h5.tl().x + pt.x, roi16h5.tl().y + pt.y);
        }
        return pts;
    }

    /**
     * Warps the image of a specific 16h5 AprilTag onto the destination image at the given points.
     *
     * @param tagId The id of the specific tag to warp onto the destination image
     * @param dstPoints Points(4) in destination image where the tag marker(black square) corners
     *     should be warped onto.
     * @param destination The destination image to place the warped tag image onto.
     * @param antialiasing If antialiasing should be performed by automatically
     *     supersampling/interpolating the warped image. This should be used if better stream quality
     *     is desired or target detection is being done on the stream, but can hurt performance.
     * @see OpenCVHelp#targetCornersToMat(org.photonvision.targeting.TargetCorner...)
     */
    public static void warp16h5TagImage(
            int tagId, MatOfPoint2f dstPoints, Mat destination, boolean antialiasing) {
        Mat tagImage = kTag16h5Images.get(tagId);
        if (tagImage == null || tagImage.empty()) return;
        var tagPoints = new MatOfPoint2f(kTag16h5MarkerPts);
        // points of tag image corners
        var tagImageCorners = new MatOfPoint2f(getImageCorners(tagImage.size()));
        // the rectangle describing the rectangle-of-interest(ROI)
        var boundingRect = Imgproc.boundingRect(dstPoints);
        // find the perspective transform from the tag image to the warped destination points
        Mat perspecTrf = Imgproc.getPerspectiveTransform(tagPoints, dstPoints);
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
        supersampling = Math.max(Math.min(supersampling, 8), 1);

        Mat scaledTagImage = new Mat();
        if (warpedTagUpscale > 2.0) {
            warpStrategy = Imgproc.INTER_LINEAR;
            int scaleFactor = (int) (warpedTagUpscale / 3.0) + 2;
            scaleFactor = Math.max(Math.min(scaleFactor, 40), 1);
            scaleFactor *= supersampling;
            Imgproc.resize(
                    tagImage, scaledTagImage, new Size(), scaleFactor, scaleFactor, Imgproc.INTER_NEAREST);
            tagPoints.fromArray(get16h5MarkerPts(scaleFactor));
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
            Core.multiply(dstPoints, new Scalar(supersampling, supersampling), scaledDstPts);
            boundingRect.x *= supersampling;
            boundingRect.y *= supersampling;
            boundingRect.width *= supersampling;
            boundingRect.height *= supersampling;
        } else dstPoints.assignTo(scaledDstPts);

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
     * Draws a contour around the given points and text of the id onto the destination image.
     *
     * @param id Fiducial ID number to draw
     * @param dstPoints Points representing the four corners of the tag marker(black square) in the
     *     destination image.
     * @param destination The destination image to draw onto. The image should be in the BGR color
     *     space.
     */
    public static void drawTagDetection(int id, MatOfPoint2f dstPoints, Mat destination) {
        var dstPointsd = new MatOfPoint(dstPoints.toArray());
        double scaleX = destination.width() / 640.0;
        double scaleY = destination.height() / 480.0;
        double minScale = Math.min(scaleX, scaleY);
        int thickness = (int) (1 * minScale);
        // for(var pt : dstPoints.toArray()) {
        //     Imgproc.circle(destination, pt, 4, new Scalar(255), 1, Imgproc.LINE_AA);
        // }
        // Imgproc.rectangle(destination, extremeRect, new Scalar(255), 1, Imgproc.LINE_AA);
        // Imgproc.rectangle(destination, Imgproc.boundingRect(dstPoints), new Scalar(255), 1,
        // Imgproc.LINE_AA);
        Imgproc.polylines(
                destination, List.of(dstPointsd), true, new Scalar(0, 0, 255), thickness, Imgproc.LINE_AA);
        var textPt = Imgproc.boundingRect(dstPoints).tl();
        textPt.x -= 10.0 * scaleX;
        textPt.y -= 12.0 * scaleY;
        Imgproc.putText(
                destination,
                String.valueOf(id),
                textPt,
                Imgproc.FONT_HERSHEY_PLAIN,
                1.5 * minScale,
                new Scalar(0, 0, 255),
                thickness,
                Imgproc.LINE_AA);
    }
}
