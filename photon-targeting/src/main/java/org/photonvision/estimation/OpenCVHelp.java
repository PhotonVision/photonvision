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

package org.photonvision.estimation;

import edu.wpi.first.cscore.CvSink;
import edu.wpi.first.math.MatBuilder;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.Num;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.numbers.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
import org.photonvision.targeting.PnpResult;
import org.photonvision.targeting.TargetCorner;

public final class OpenCVHelp {
    private static final Rotation3d NWU_TO_EDN;
    private static final Rotation3d EDN_TO_NWU;

    // Creating a cscore object is sufficient to load opencv, per
    // https://www.chiefdelphi.com/t/unsatisfied-link-error-when-simulating-java-robot-code-using-opencv/426731/4
    private static CvSink dummySink = null;

    public static void forceLoadOpenCV() {
        if (dummySink != null) return;
        dummySink = new CvSink("ignored");
        dummySink.close();
    }

    static {
        NWU_TO_EDN = new Rotation3d(MatBuilder.fill(Nat.N3(), Nat.N3(), 0, -1, 0, 0, 0, -1, 1, 0, 0));
        EDN_TO_NWU = new Rotation3d(MatBuilder.fill(Nat.N3(), Nat.N3(), 0, 0, 1, -1, 0, 0, 0, -1, 0));
    }

    public static Mat matrixToMat(SimpleMatrix matrix) {
        var mat = new Mat(matrix.getNumRows(), matrix.getNumCols(), CvType.CV_64F);
        mat.put(0, 0, matrix.getDDRM().getData());
        return mat;
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
            var trl = translationNWUtoEDN(translations[i]);
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
        return translationEDNtoNWU(new Translation3d(data[0], data[1], data[2]));
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
        // Get the 'rodriguez' (axis-angle, where the norm is the angle about the normalized direction
        // of the vector)
        float[] data = new float[3];
        var wrapped = new Mat(rvecInput.rows(), rvecInput.cols(), CvType.CV_32F);
        rvecInput.convertTo(wrapped, CvType.CV_32F);
        wrapped.get(0, 0, data);
        wrapped.release();

        return rotationEDNtoNWU(new Rotation3d(VecBuilder.fill(data[0], data[1], data[2])));
    }

    public static Point avgPoint(Point[] points) {
        if (points == null || points.length == 0) return null;
        var pointMat = new MatOfPoint2f(points);
        Core.reduce(pointMat, pointMat, 0, Core.REDUCE_AVG);
        var avgPt = pointMat.toArray()[0];
        pointMat.release();
        return avgPt;
    }

    public static Point[] cornersToPoints(List<TargetCorner> corners) {
        var points = new Point[corners.size()];
        for (int i = 0; i < corners.size(); i++) {
            var corn = corners.get(i);
            points[i] = new Point(corn.x, corn.y);
        }
        return points;
    }

    public static Point[] cornersToPoints(TargetCorner... corners) {
        var points = new Point[corners.length];
        for (int i = 0; i < corners.length; i++) {
            points[i] = new Point(corners[i].x, corners[i].y);
        }
        return points;
    }

    public static List<TargetCorner> pointsToCorners(Point... points) {
        var corners = new ArrayList<TargetCorner>(points.length);
        for (Point point : points) {
            corners.add(new TargetCorner(point.x, point.y));
        }
        return corners;
    }

    public static List<TargetCorner> pointsToCorners(MatOfPoint2f matInput) {
        var corners = new ArrayList<TargetCorner>();
        float[] data = new float[(int) matInput.total() * matInput.channels()];
        matInput.get(0, 0, data);
        for (int i = 0; i < (int) matInput.total(); i++) {
            corners.add(new TargetCorner(data[0 + 2 * i], data[1 + 2 * i]));
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
     * @param elements list elements
     * @param backwards If indexing should happen in reverse (0, size-1, size-2, ...)
     * @param shiftStart How much the initial index should be shifted (instead of starting at index 0,
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
     * Convert a rotation delta from EDN to NWU. For example, if you have a rotation X,Y,Z {1, 0, 0}
     * in EDN, this would be {0, -1, 0} in NWU.
     */
    private static Rotation3d rotationEDNtoNWU(Rotation3d rot) {
        return EDN_TO_NWU.unaryMinus().plus(rot.plus(EDN_TO_NWU));
    }

    /**
     * Convert a rotation delta from NWU to EDN. For example, if you have a rotation X,Y,Z {1, 0, 0}
     * in NWU, this would be {0, 0, 1} in EDN.
     */
    private static Rotation3d rotationNWUtoEDN(Rotation3d rot) {
        return NWU_TO_EDN.unaryMinus().plus(rot.plus(NWU_TO_EDN));
    }

    /**
     * Convert a translation from EDN to NWU. For example, if you have a translation X,Y,Z {1, 0, 0}
     * in EDN, this would be {0, -1, 0} in NWU.
     */
    private static Translation3d translationEDNtoNWU(Translation3d trl) {
        return trl.rotateBy(EDN_TO_NWU);
    }

    /**
     * Convert a translation from NWU to EDN. For example, if you have a translation X,Y,Z {1, 0, 0}
     * in NWU, this would be {0, 0, 1} in EDN.
     */
    private static Translation3d translationNWUtoEDN(Translation3d trl) {
        return trl.rotateBy(NWU_TO_EDN);
    }

    /**
     * Distort a list of points in pixels using the OPENCV5/8 models. See image-rotation.md or
     * https://docs.opencv.org/4.x/d9/d0c/group__calib3d.html for the math here.
     *
     * @param pointsList the undistorted points
     * @param cameraMatrix standard OpenCV camera mat
     * @param distCoeffs standard OpenCV distortion coefficients. Must OPENCV5 or OPENCV8
     */
    public static List<Point> distortPoints(
            List<Point> pointsList, Mat cameraMatrix, Mat distCoeffs) {
        var ret = new ArrayList<Point>();

        var cx = cameraMatrix.get(0, 2)[0];
        var cy = cameraMatrix.get(1, 2)[0];
        var fx = cameraMatrix.get(0, 0)[0];
        var fy = cameraMatrix.get(1, 1)[0];

        var k1 = distCoeffs.get(0, 0)[0];
        var k2 = distCoeffs.get(0, 1)[0];
        var p1 = distCoeffs.get(0, 2)[0];
        var p2 = distCoeffs.get(0, 3)[0];
        var k3 = distCoeffs.get(0, 4)[0];

        double k4 = 0;
        double k5 = 0;
        double k6 = 0;
        if (distCoeffs.cols() == 8) {
            k4 = distCoeffs.get(0, 5)[0];
            k5 = distCoeffs.get(0, 6)[0];
            k6 = distCoeffs.get(0, 7)[0];
        }

        for (Point point : pointsList) {
            // To relative coordinates
            double xprime = (point.x - cx) / fx; // cx, cy is the center of distortion
            double yprime = (point.y - cy) / fy;

            double r_sq = xprime * xprime + yprime * yprime; // square of the radius from center

            // Radial distortion
            double radialDistortion =
                    (1 + k1 * r_sq + k2 * r_sq * r_sq + k3 * r_sq * r_sq * r_sq)
                            / (1 + k4 * r_sq + k5 * r_sq * r_sq + k6 * r_sq * r_sq * r_sq);
            double xDistort = xprime * radialDistortion;
            double yDistort = yprime * radialDistortion;

            // Tangential distortion
            xDistort = xDistort + (2 * p1 * xprime * yprime + p2 * (r_sq + 2 * xprime * xprime));
            yDistort = yDistort + (p1 * (r_sq + 2 * yprime * yprime) + 2 * p2 * xprime * yprime);

            // Back to absolute coordinates.
            xDistort = xDistort * fx + cx;
            yDistort = yDistort * fy + cy;
            ret.add(new Point(xDistort, yDistort));
        }

        return ret;
    }

    /**
     * Project object points from the 3d world into the 2d camera image. The camera
     * properties(intrinsics, distortion) determine the results of this projection.
     *
     * @param cameraMatrix the camera intrinsics matrix in standard opencv form
     * @param distCoeffs the camera distortion matrix in standard opencv form
     * @param camRt The change in basis from world coordinates to camera coordinates. See {@link
     *     RotTrlTransform3d#makeRelativeTo(Pose3d)}.
     * @param objectTranslations The 3d points to be projected
     * @return The 2d points in pixels which correspond to the camera's image of the 3d points
     */
    public static Point[] projectPoints(
            Matrix<N3, N3> cameraMatrix,
            Matrix<N8, N1> distCoeffs,
            RotTrlTransform3d camRt,
            List<Translation3d> objectTranslations) {
        // translate to opencv classes
        var objectPoints = translationToTvec(objectTranslations.toArray(new Translation3d[0]));
        // opencv rvec/tvec describe a change in basis from world to camera
        var rvec = rotationToRvec(camRt.getRotation());
        var tvec = translationToTvec(camRt.getTranslation());
        var cameraMatrixMat = matrixToMat(cameraMatrix.getStorage());
        var distCoeffsMat = new MatOfDouble(matrixToMat(distCoeffs.getStorage()));
        var imagePoints = new MatOfPoint2f();
        // project to 2d
        Calib3d.projectPoints(objectPoints, rvec, tvec, cameraMatrixMat, distCoeffsMat, imagePoints);
        var points = imagePoints.toArray();

        // release our Mats from native memory
        objectPoints.release();
        rvec.release();
        tvec.release();
        cameraMatrixMat.release();
        distCoeffsMat.release();
        imagePoints.release();

        return points;
    }

    /**
     * Undistort 2d image points using a given camera's intrinsics and distortion.
     *
     * <p>2d image points from {@link #projectPoints(Matrix, Matrix, RotTrlTransform3d, List)
     * projectPoints()} will naturally be distorted, so this operation is important if the image
     * points need to be directly used (e.g. 2d yaw/pitch).
     *
     * @param cameraMatrix The camera intrinsics matrix in standard opencv form
     * @param distCoeffs The camera distortion matrix in standard opencv form
     * @param points The distorted image points
     * @return The undistorted image points
     */
    public static Point[] undistortPoints(
            Matrix<N3, N3> cameraMatrix, Matrix<N8, N1> distCoeffs, Point[] points) {
        var distMat = new MatOfPoint2f(points);
        var undistMat = new MatOfPoint2f();
        var cameraMatrixMat = matrixToMat(cameraMatrix.getStorage());
        var distCoeffsMat = matrixToMat(distCoeffs.getStorage());

        Calib3d.undistortImagePoints(distMat, undistMat, cameraMatrixMat, distCoeffsMat);
        var undistPoints = undistMat.toArray();

        distMat.release();
        undistMat.release();
        cameraMatrixMat.release();
        distCoeffsMat.release();

        return undistPoints;
    }

    /**
     * Gets the (upright) rectangle which bounds this contour.
     *
     * <p>Note that rectangle size and position are stored with ints and do not have sub-pixel
     * accuracy.
     *
     * @param points The points to be bounded
     * @return Rectangle bounding the given points
     */
    public static Rect getBoundingRect(Point[] points) {
        var pointMat = new MatOfPoint2f(points);
        var rect = Imgproc.boundingRect(pointMat);
        pointMat.release();
        return rect;
    }

    /**
     * Gets the rotated rectangle with minimum area which bounds this contour.
     *
     * <p>Note that rectangle size and position are stored with floats and have sub-pixel accuracy.
     *
     * @param points The points to be bounded
     * @return Rotated rectangle bounding the given points
     */
    public static RotatedRect getMinAreaRect(Point[] points) {
        var pointMat = new MatOfPoint2f(points);
        var rect = Imgproc.minAreaRect(pointMat);
        pointMat.release();
        return rect;
    }

    /**
     * Gets the convex hull contour (the outline) of a list of points.
     *
     * @param points The input contour
     * @return The subset of points defining the convex hull. Note that these use ints and not floats.
     */
    public static Point[] getConvexHull(Point[] points) {
        var pointMat = new MatOfPoint(points);
        // outputHull gives us indices (of corn) that make a convex hull contour
        var outputHull = new MatOfInt();

        Imgproc.convexHull(pointMat, outputHull);

        int[] indices = outputHull.toArray();
        outputHull.release();
        pointMat.release();
        var convexPoints = new Point[indices.length];
        for (int i = 0; i < indices.length; i++) {
            convexPoints[i] = points[indices[i]];
        }
        return convexPoints;
    }

    /**
     * Finds the transformation(s) that map the camera's pose to the target's pose. The camera's pose
     * relative to the target is determined by the supplied 3d points of the target's model and their
     * associated 2d points imaged by the camera. The supplied model translations must be relative to
     * the target's pose.
     *
     * <p>For planar targets, there may be an alternate solution which is plausible given the 2d image
     * points. This has an associated "ambiguity" which describes the ratio of reprojection error
     * between the "best" and "alternate" solution.
     *
     * <p>This method is intended for use with individual AprilTags, and will not work unless 4 points
     * are provided.
     *
     * @param cameraMatrix The camera intrinsics matrix in standard opencv form
     * @param distCoeffs The camera distortion matrix in standard opencv form
     * @param modelTrls The translations of the object corners. These should have the object pose as
     *     their origin. These must come in a specific, pose-relative order (in NWU):
     *     <ul>
     *       <li>Point 0: [0, -squareLength / 2, squareLength / 2]
     *       <li>Point 1: [0, squareLength / 2, squareLength / 2]
     *       <li>Point 2: [0, squareLength / 2, -squareLength / 2]
     *       <li>Point 3: [0, -squareLength / 2, -squareLength / 2]
     *     </ul>
     *
     * @param imagePoints The projection of these 3d object points into the 2d camera image. The order
     *     should match the given object point translations.
     * @return The resulting transformation that maps the camera pose to the target pose and the
     *     ambiguity if an alternate solution is available.
     */
    public static Optional<PnpResult> solvePNP_SQUARE(
            Matrix<N3, N3> cameraMatrix,
            Matrix<N8, N1> distCoeffs,
            List<Translation3d> modelTrls,
            Point[] imagePoints) {
        // solvepnp inputs
        MatOfPoint3f objectMat = new MatOfPoint3f();
        MatOfPoint2f imageMat = new MatOfPoint2f();
        MatOfDouble cameraMatrixMat = new MatOfDouble();
        MatOfDouble distCoeffsMat = new MatOfDouble();
        var rvecs = new ArrayList<Mat>();
        var tvecs = new ArrayList<Mat>();
        Mat rvec = Mat.zeros(3, 1, CvType.CV_32F);
        Mat tvec = Mat.zeros(3, 1, CvType.CV_32F);
        Mat reprojectionError = Mat.zeros(2, 1, CvType.CV_32F);
        try {
            // IPPE_SQUARE expects our corners in a specific order
            modelTrls = reorderCircular(modelTrls, true, -1);
            imagePoints = reorderCircular(Arrays.asList(imagePoints), true, -1).toArray(Point[]::new);
            // translate to opencv classes
            translationToTvec(modelTrls.toArray(new Translation3d[0])).assignTo(objectMat);
            imageMat.fromArray(imagePoints);
            matrixToMat(cameraMatrix.getStorage()).assignTo(cameraMatrixMat);
            matrixToMat(distCoeffs.getStorage()).assignTo(distCoeffsMat);

            float[] errors = new float[2];
            Transform3d best = null;
            Transform3d alt = null;

            for (int tries = 0; tries < 2; tries++) {
                // calc rvecs/tvecs and associated reprojection error from image points
                Calib3d.solvePnPGeneric(
                        objectMat,
                        imageMat,
                        cameraMatrixMat,
                        distCoeffsMat,
                        rvecs,
                        tvecs,
                        false,
                        Calib3d.SOLVEPNP_IPPE_SQUARE,
                        rvec,
                        tvec,
                        reprojectionError);

                reprojectionError.get(0, 0, errors);
                // convert to wpilib coordinates
                best = new Transform3d(tvecToTranslation(tvecs.get(0)), rvecToRotation(rvecs.get(0)));

                if (tvecs.size() > 1) {
                    alt = new Transform3d(tvecToTranslation(tvecs.get(1)), rvecToRotation(rvecs.get(1)));
                }

                // check if we got a NaN result
                if (!Double.isNaN(errors[0])) break;
                else { // add noise and retry
                    double[] br = imageMat.get(0, 0);
                    br[0] -= 0.001;
                    br[1] -= 0.001;
                    imageMat.put(0, 0, br);
                }
            }

            // check if solvePnP failed with NaN results and retrying failed
            if (Double.isNaN(errors[0])) throw new Exception("SolvePNP_SQUARE NaN result");

            if (alt != null)
                return Optional.of(new PnpResult(best, alt, errors[0] / errors[1], errors[0], errors[1]));
            else return Optional.empty();
        }
        // solvePnP failed
        catch (Exception e) {
            System.err.println("SolvePNP_SQUARE failed!");
            e.printStackTrace();
            return Optional.empty();
        } finally {
            // release our Mats from native memory
            objectMat.release();
            imageMat.release();
            cameraMatrixMat.release();
            distCoeffsMat.release();
            for (var v : rvecs) v.release();
            for (var v : tvecs) v.release();
            rvec.release();
            tvec.release();
            reprojectionError.release();
        }
    }

    /**
     * Finds the transformation that maps the camera's pose to the origin of the supplied object. An
     * "object" is simply a set of known 3d translations that correspond to the given 2d points. If,
     * for example, the object translations are given relative to close-right corner of the blue
     * alliance(the default origin), a camera-to-origin transformation is returned. If the
     * translations are relative to a target's pose, a camera-to-target transformation is returned.
     *
     * <p>There must be at least 3 points to use this method. This does not return an alternate
     * solution-- if you are intending to use solvePNP on a single AprilTag, see {@link
     * #solvePNP_SQUARE} instead.
     *
     * @param cameraMatrix The camera intrinsics matrix in standard opencv form
     * @param distCoeffs The camera distortion matrix in standard opencv form
     * @param objectTrls The translations of the object corners, relative to the field.
     * @param imagePoints The projection of these 3d object points into the 2d camera image. The order
     *     should match the given object point translations.
     * @return The resulting transformation that maps the camera pose to the target pose. If the 3d
     *     model points are supplied relative to the origin, this transformation brings the camera to
     *     the origin.
     */
    public static Optional<PnpResult> solvePNP_SQPNP(
            Matrix<N3, N3> cameraMatrix,
            Matrix<N8, N1> distCoeffs,
            List<Translation3d> objectTrls,
            Point[] imagePoints) {
        try {
            // translate to opencv classes
            MatOfPoint3f objectMat = translationToTvec(objectTrls.toArray(new Translation3d[0]));
            MatOfPoint2f imageMat = new MatOfPoint2f(imagePoints);
            Mat cameraMatrixMat = matrixToMat(cameraMatrix.getStorage());
            Mat distCoeffsMat = matrixToMat(distCoeffs.getStorage());
            var rvecs = new ArrayList<Mat>();
            var tvecs = new ArrayList<Mat>();
            Mat rvec = Mat.zeros(3, 1, CvType.CV_32F);
            Mat tvec = Mat.zeros(3, 1, CvType.CV_32F);
            Mat reprojectionError = new Mat();
            // calc rvec/tvec from image points
            Calib3d.solvePnPGeneric(
                    objectMat,
                    imageMat,
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
            objectMat.release();
            imageMat.release();
            cameraMatrixMat.release();
            distCoeffsMat.release();
            for (var v : rvecs) v.release();
            for (var v : tvecs) v.release();
            rvec.release();
            tvec.release();
            reprojectionError.release();

            // check if solvePnP failed with NaN results
            if (Double.isNaN(error[0])) throw new Exception("SolvePNP_SQPNP NaN result");

            return Optional.of(new PnpResult(best, error[0]));
        } catch (Exception e) {
            System.err.println("SolvePNP_SQPNP failed!");
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
