/*
 * MIT License
 *
 * Copyright (c) 2023 PhotonVision
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

package org.photonvision.estimation;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.Num;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.geometry.CoordinateSystem;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.numbers.*;
import edu.wpi.first.util.RuntimeLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.ejml.simple.SimpleMatrix;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;
import org.photonvision.targeting.TargetCorner;

public final class OpenCVHelp {
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

    public static MatOfDouble matrixToMat(SimpleMatrix matrix) {
        var mat = new Mat(matrix.numRows(), matrix.numCols(), CvType.CV_64F);
        mat.put(0, 0, matrix.getDDRM().getData());
        var wrappedMat = new MatOfDouble();
        mat.convertTo(wrappedMat, CvType.CV_64F);
        mat.release();
        return wrappedMat;
    }

    public static Matrix<Num, Num> matToMatrix(Mat mat) {
        double[] data = new double[(int) mat.total() * mat.channels()];
        var doubleMat = new Mat(mat.rows(), mat.cols(), CvType.CV_64F);
        mat.convertTo(doubleMat, CvType.CV_64F);
        doubleMat.get(0, 0, data);
        return new Matrix<>(new SimpleMatrix(mat.rows(), mat.cols(), true, data));
    }

    /**
     * Creates a new {@link MatOfPoint3f} with these 3d translations. The opencv tvec is a vector with
     * three elements representing {x, y, z} in the EDN coordinate system.
     *
     * @param translations The translations to convert into a MatOfPoint3f
     */
    public static MatOfPoint3f translationToTvec(Translation3d... translations) {
        Point3[] points = new Point3[translations.length];
        for (int i = 0; i < translations.length; i++) {
            var trl =
                    CoordinateSystem.convert(translations[i], CoordinateSystem.NWU(), CoordinateSystem.EDN());
            points[i] = new Point3(trl.getX(), trl.getY(), trl.getZ());
        }
        return new MatOfPoint3f(points);
    }
    /**
     * Returns a new 3d translation from this {@link Mat}. The opencv tvec is a vector with three
     * elements representing {x, y, z} in the EDN coordinate system.
     *
     * @param tvecInput The tvec to create a Translation3d from
     */
    public static Translation3d tvecToTranslation(Mat tvecInput) {
        float[] data = new float[3];
        var wrapped = new Mat(tvecInput.rows(), tvecInput.cols(), CvType.CV_32F);
        tvecInput.convertTo(wrapped, CvType.CV_32F);
        wrapped.get(0, 0, data);
        wrapped.release();
        return CoordinateSystem.convert(
                new Translation3d(data[0], data[1], data[2]),
                CoordinateSystem.EDN(),
                CoordinateSystem.NWU());
    }

    /**
     * Creates a new {@link MatOfPoint3f} with this 3d rotation. The opencv rvec Mat is a vector with
     * three elements representing the axis scaled by the angle in the EDN coordinate system. (angle =
     * norm, and axis = rvec / norm)
     *
     * @param rotation The rotation to convert into a MatOfPoint3f
     */
    public static MatOfPoint3f rotationToRvec(Rotation3d rotation) {
        rotation = rotationNWUtoEDN(rotation);
        return new MatOfPoint3f(new Point3(rotation.getQuaternion().toRotationVector().getData()));
    }
    /**
     * Returns a 3d rotation from this {@link Mat}. The opencv rvec Mat is a vector with three
     * elements representing the axis scaled by the angle in the EDN coordinate system. (angle = norm,
     * and axis = rvec / norm)
     *
     * @param rvecInput The rvec to create a Rotation3d from
     */
    public static Rotation3d rvecToRotation(Mat rvecInput) {
        float[] data = new float[3];
        var wrapped = new Mat(rvecInput.rows(), rvecInput.cols(), CvType.CV_32F);
        rvecInput.convertTo(wrapped, CvType.CV_32F);
        wrapped.get(0, 0, data);
        wrapped.release();
        Vector<N3> axis = new Vector<>(Nat.N3());
        axis.set(0, 0, data[0]);
        axis.set(1, 0, data[1]);
        axis.set(2, 0, data[2]);
        return rotationEDNtoNWU(new Rotation3d(axis.div(axis.norm()), axis.norm()));
    }

    public static TargetCorner averageCorner(List<TargetCorner> corners) {
        if (corners == null || corners.size() == 0) return null;

        var pointMat = targetCornersToMat(corners);
        Core.reduce(pointMat, pointMat, 0, Core.REDUCE_AVG);
        var avgPt = matToTargetCorners(pointMat)[0];
        pointMat.release();
        return avgPt;
    }

    public static MatOfPoint2f targetCornersToMat(List<TargetCorner> corners) {
        return targetCornersToMat(corners.toArray(TargetCorner[]::new));
    }

    public static MatOfPoint2f targetCornersToMat(TargetCorner... corners) {
        var points = new Point[corners.length];
        for (int i = 0; i < corners.length; i++) {
            points[i] = new Point(corners[i].x, corners[i].y);
        }
        return new MatOfPoint2f(points);
    }

    public static TargetCorner[] pointsToTargetCorners(Point... points) {
        var corners = new TargetCorner[points.length];
        for (int i = 0; i < points.length; i++) {
            corners[i] = new TargetCorner(points[i].x, points[i].y);
        }
        return corners;
    }

    public static TargetCorner[] matToTargetCorners(MatOfPoint2f matInput) {
        var corners = new TargetCorner[(int) matInput.total()];
        float[] data = new float[(int) matInput.total() * matInput.channels()];
        matInput.get(0, 0, data);
        for (int i = 0; i < corners.length; i++) {
            corners[i] = new TargetCorner(data[0 + 2 * i], data[1 + 2 * i]);
        }
        return corners;
    }

    /**
     * Reorders the list, optionally indexing backwards and wrapping around to the last element after
     * the first, and shifting all indices in the direction of indexing.
     *
     * <p>e.g.
     *
     * <p>({1,2,3}, false, 1) == {2,3,1}
     *
     * <p>({1,2,3}, true, 0) == {1,3,2}
     *
     * <p>({1,2,3}, true, 1) == {3,2,1}
     *
     * @param <T> Element type
     * @param elements
     * @param backwards If indexing should happen in reverse (0, size-1, size-2, ...)
     * @param shiftStart How much the inital index should be shifted (instead of starting at index 0,
     *     start at shiftStart, negated if backwards)
     * @return Reordered list
     */
    public static <T> List<T> reorderCircular(List<T> elements, boolean backwards, int shiftStart) {
        int size = elements.size();
        int dir = backwards ? -1 : 1;
        var reordered = new ArrayList<>(elements);
        for (int i = 0; i < size; i++) {
            int index = (i * dir + shiftStart * dir) % size;
            if (index < 0) index = size + index;
            reordered.set(i, elements.get(index));
        }
        return reordered;
    }

    /**
     * Convert a rotation from EDN to NWU. For example, if you have a rotation X,Y,Z {1, 0, 0} in EDN,
     * this would be XYZ {0, -1, 0} in NWU.
     */
    private static Rotation3d rotationEDNtoNWU(Rotation3d rot) {
        return CoordinateSystem.convert(
                        new Rotation3d(), CoordinateSystem.NWU(), CoordinateSystem.EDN())
                .plus(CoordinateSystem.convert(rot, CoordinateSystem.EDN(), CoordinateSystem.NWU()));
    }
    /**
     * Convert a rotation from EDN to NWU. For example, if you have a rotation X,Y,Z {1, 0, 0} in EDN,
     * this would be XYZ {0, -1, 0} in NWU.
     */
    private static Rotation3d rotationNWUtoEDN(Rotation3d rot) {
        return CoordinateSystem.convert(
                        new Rotation3d(), CoordinateSystem.EDN(), CoordinateSystem.NWU())
                .plus(CoordinateSystem.convert(rot, CoordinateSystem.NWU(), CoordinateSystem.EDN()));
    }

    /**
     * Project object points from the 3d world into the 2d camera image. The camera
     * properties(intrinsics, distortion) determine the results of this projection.
     *
     * @param cameraMatrix the camera intrinsics matrix in standard opencv form
     * @param distCoeffs the camera distortion matrix in standard opencv form
     * @param camPose The current camera pose in the 3d world
     * @param objectTranslations The 3d points to be projected
     * @return The 2d points in pixels which correspond to the image of the 3d points on the camera
     */
    public static List<TargetCorner> projectPoints(
            Matrix<N3, N3> cameraMatrix,
            Matrix<N5, N1> distCoeffs,
            Pose3d camPose,
            List<Translation3d> objectTranslations) {
        // translate to opencv classes
        var objectPoints = translationToTvec(objectTranslations.toArray(new Translation3d[0]));
        // opencv rvec/tvec describe a change in basis from world to camera
        var basisChange = RotTrlTransform3d.makeRelativeTo(camPose);
        var rvec = rotationToRvec(basisChange.getRotation());
        var tvec = translationToTvec(basisChange.getTranslation());
        var cameraMatrixMat = matrixToMat(cameraMatrix.getStorage());
        var distCoeffsMat = matrixToMat(distCoeffs.getStorage());
        var imagePoints = new MatOfPoint2f();
        // project to 2d
        Calib3d.projectPoints(objectPoints, rvec, tvec, cameraMatrixMat, distCoeffsMat, imagePoints);

        // turn 2d point Mat into TargetCorners
        var corners = matToTargetCorners(imagePoints);

        // release our Mats from native memory
        objectPoints.release();
        rvec.release();
        tvec.release();
        cameraMatrixMat.release();
        distCoeffsMat.release();
        imagePoints.release();

        return Arrays.asList(corners);
    }

    /**
     * Undistort 2d image points using a given camera's intrinsics and distortion.
     *
     * <p>2d image points from projectPoints(CameraProperties, Pose3d, List) projectPoints} will
     * naturally be distorted, so this operation is important if the image points need to be directly
     * used (e.g. 2d yaw/pitch).
     *
     * @param cameraMatrix the camera intrinsics matrix in standard opencv form
     * @param distCoeffs the camera distortion matrix in standard opencv form
     * @param corners The distorted image points
     * @return The undistorted image points
     */
    public static List<TargetCorner> undistortPoints(
            Matrix<N3, N3> cameraMatrix, Matrix<N5, N1> distCoeffs, List<TargetCorner> corners) {
        var points_in = targetCornersToMat(corners);
        var points_out = new MatOfPoint2f();
        var cameraMatrixMat = matrixToMat(cameraMatrix.getStorage());
        var distCoeffsMat = matrixToMat(distCoeffs.getStorage());

        Calib3d.undistortImagePoints(points_in, points_out, cameraMatrixMat, distCoeffsMat);
        var corners_out = matToTargetCorners(points_out);

        points_in.release();
        points_out.release();
        cameraMatrixMat.release();
        distCoeffsMat.release();

        return Arrays.asList(corners_out);
    }

    /**
     * Gets the (upright) rectangle which bounds this contour.
     *
     * <p>Note that rectangle size and position are stored with ints and do not have sub-pixel
     * accuracy.
     *
     * @param corners The corners/points to be bounded
     * @return Rectangle bounding the given corners
     */
    public static Rect getBoundingRect(List<TargetCorner> corners) {
        var corn = targetCornersToMat(corners);
        var rect = Imgproc.boundingRect(corn);
        corn.release();
        return rect;
    }
    /**
     * Gets the rotated rectangle with minimum area which bounds this contour.
     *
     * <p>Note that rectangle size and position are stored with doubles and have sub-pixel accuracy.
     *
     * @param corners The corners/points to be bounded
     * @return Rotated rectangle bounding the given corners
     */
    public static RotatedRect getMinAreaRect(List<TargetCorner> corners) {
        var corn = targetCornersToMat(corners);
        var rect = Imgproc.minAreaRect(corn);
        corn.release();
        return rect;
    }
    /**
     * Get the area in pixels of this target's contour. It's important to note that this may be
     * different from the area of the bounding rectangle around the contour.
     *
     * @param corners The corners defining this contour
     * @return Area in pixels (units of corner x/y)
     */
    public static double getContourAreaPx(List<TargetCorner> corners) {
        var temp = targetCornersToMat(corners);
        var corn = new MatOfPoint(temp.toArray());
        temp.release();

        // outputHull gives us indices (of corn) that make a convex hull contour
        var outputHull = new MatOfInt();
        Imgproc.convexHull(corn, outputHull);
        int[] indices = outputHull.toArray();
        outputHull.release();
        var tempPoints = corn.toArray();
        var points = tempPoints.clone();
        for (int i = 0; i < indices.length; i++) {
            points[i] = tempPoints[indices[i]];
        }
        corn.fromArray(points);
        // calculate area of the (convex hull) contour
        double area = Imgproc.contourArea(corn);
        corn.release();
        return area;
    }

    /**
     * Finds the transformation(s) that map the camera's pose to the target pose. The camera's pose
     * relative to the target is determined by the supplied 3d points of the target's model and their
     * associated 2d points imaged by the camera.
     *
     * <p>For planar targets, there may be an alternate solution which is plausible given the 2d image
     * points. This has an associated "ambiguity" which describes the ratio of reprojection error
     * between the "best" and "alternate" solution.
     *
     * <p>This method is intended for use with individual AprilTags, and will not work unless 4 points
     * are provided.
     *
     * @param cameraMatrix the camera intrinsics matrix in standard opencv form
     * @param distCoeffs the camera distortion matrix in standard opencv form
     * @param modelTrls The translations of the object corners. These should have the object pose as
     *     their origin. These must come in a specific, pose-relative order (in NWU):
     *     <ul>
     *       <li>Point 0: [0, -squareLength / 2, squareLength / 2]
     *       <li>Point 1: [0, squareLength / 2, squareLength / 2]
     *       <li>Point 2: [0, squareLength / 2, -squareLength / 2]
     *       <li>Point 3: [0, -squareLength / 2, -squareLength / 2]
     *     </ul>
     *
     * @param imageCorners The projection of these 3d object points into the 2d camera image. The
     *     order should match the given object point translations.
     * @return The resulting transformation that maps the camera pose to the target pose and the
     *     ambiguity if an alternate solution is available.
     */
    public static PNPResults solvePNP_SQUARE(
            Matrix<N3, N3> cameraMatrix,
            Matrix<N5, N1> distCoeffs,
            List<Translation3d> modelTrls,
            List<TargetCorner> imageCorners) {
        // IPPE_SQUARE expects our corners in a specific order
        modelTrls = reorderCircular(modelTrls, true, -1);
        imageCorners = reorderCircular(imageCorners, true, -1);
        // translate to opencv classes
        var objectPoints = translationToTvec(modelTrls.toArray(new Translation3d[0]));
        var imagePoints = targetCornersToMat(imageCorners);
        var cameraMatrixMat = matrixToMat(cameraMatrix.getStorage());
        var distCoeffsMat = matrixToMat(distCoeffs.getStorage());
        var rvecs = new ArrayList<Mat>();
        var tvecs = new ArrayList<Mat>();
        var rvec = Mat.zeros(3, 1, CvType.CV_32F);
        var tvec = Mat.zeros(3, 1, CvType.CV_32F);
        var reprojectionError = new Mat();
        // calc rvecs/tvecs and associated reprojection error from image points
        Calib3d.solvePnPGeneric(
                objectPoints,
                imagePoints,
                cameraMatrixMat,
                distCoeffsMat,
                rvecs,
                tvecs,
                false,
                Calib3d.SOLVEPNP_IPPE_SQUARE,
                rvec,
                tvec,
                reprojectionError);

        float[] errors = new float[2];
        reprojectionError.get(0, 0, errors);
        // convert to wpilib coordinates
        var best = new Transform3d(tvecToTranslation(tvecs.get(0)), rvecToRotation(rvecs.get(0)));

        Transform3d alt = null;
        if (tvecs.size() > 1) {
            alt = new Transform3d(tvecToTranslation(tvecs.get(1)), rvecToRotation(rvecs.get(1)));
        }

        // release our Mats from native memory
        objectPoints.release();
        imagePoints.release();
        cameraMatrixMat.release();
        distCoeffsMat.release();
        for (var v : rvecs) v.release();
        for (var v : tvecs) v.release();
        rvec.release();
        tvec.release();
        reprojectionError.release();

        if (alt != null) return new PNPResults(best, alt, errors[0] / errors[1], errors[0], errors[1]);
        else return new PNPResults(best, errors[0]);
    }
    /**
     * Finds the transformation that maps the camera's pose to the target pose. The camera's pose
     * relative to the target is determined by the supplied 3d points of the target's model and their
     * associated 2d points imaged by the camera.
     *
     * <p>This method is intended for use with multiple targets and has no alternate solutions. There
     * must be at least 3 points.
     *
     * @param cameraMatrix the camera intrinsics matrix in standard opencv form
     * @param distCoeffs the camera distortion matrix in standard opencv form
     * @param objectTrls The translations of the object corners, relative to the field.
     * @param imageCorners The projection of these 3d object points into the 2d camera image. The
     *     order should match the given object point translations.
     * @return The resulting transformation that maps the camera pose to the target pose. If the 3d
     *     model points are supplied relative to the origin, this transformation brings the camera to
     *     the origin.
     */
    public static PNPResults solvePNP_SQPNP(
            Matrix<N3, N3> cameraMatrix,
            Matrix<N5, N1> distCoeffs,
            List<Translation3d> objectTrls,
            List<TargetCorner> imageCorners) {
        // translate to opencv classes
        var objectPoints = translationToTvec(objectTrls.toArray(new Translation3d[0]));
        var imagePoints = targetCornersToMat(imageCorners);
        var cameraMatrixMat = matrixToMat(cameraMatrix.getStorage());
        var distCoeffsMat = matrixToMat(distCoeffs.getStorage());
        var rvecs = new ArrayList<Mat>();
        var tvecs = new ArrayList<Mat>();
        var rvec = Mat.zeros(3, 1, CvType.CV_32F);
        var tvec = Mat.zeros(3, 1, CvType.CV_32F);
        var reprojectionError = new Mat();
        // calc rvec/tvec from image points
        Calib3d.solvePnPGeneric(
                objectPoints,
                imagePoints,
                cameraMatrixMat,
                distCoeffsMat,
                rvecs,
                tvecs,
                false,
                Calib3d.SOLVEPNP_SQPNP,
                rvec,
                tvec,
                reprojectionError);

        float[] error = new float[1];
        reprojectionError.get(0, 0, error);
        // convert to wpilib coordinates
        var best = new Transform3d(tvecToTranslation(tvecs.get(0)), rvecToRotation(rvecs.get(0)));

        // release our Mats from native memory
        objectPoints.release();
        imagePoints.release();
        cameraMatrixMat.release();
        distCoeffsMat.release();
        for (var v : rvecs) v.release();
        for (var v : tvecs) v.release();
        rvec.release();
        tvec.release();
        reprojectionError.release();

        return new PNPResults(best, error[0]);
    }
}
