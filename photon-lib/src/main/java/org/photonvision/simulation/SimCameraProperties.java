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

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.Pair;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.numbers.*;
import edu.wpi.first.wpilibj.DriverStation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.ejml.data.DMatrix3;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.opencv.imgproc.Imgproc;
import org.photonvision.estimation.OpenCVHelp;
import org.photonvision.estimation.RotTrlTransform3d;
import org.photonvision.targeting.TargetCorner;

/**
 * Calibration and performance values for this camera.
 *
 * <p>The resolution will affect the accuracy of projected(3d to 2d) target corners and similarly
 * the severity of image noise on estimation(2d to 3d).
 *
 * <p>The camera intrinsics and distortion coefficients describe the results of calibration, and how
 * to map between 3d field points and 2d image points.
 *
 * <p>The performance values (framerate/exposure time, latency) determine how often results should
 * be updated and with how much latency in simulation. High exposure time causes motion blur which
 * can inhibit target detection while moving. Note that latency estimation does not account for
 * network latency and the latency reported will always be perfect.
 */
public class SimCameraProperties {
    private final Random rand = new Random();
    // calibration
    private int resWidth;
    private int resHeight;
    private Matrix<N3, N3> camIntrinsics;
    private Matrix<N5, N1> distCoeffs;
    private double avgErrorPx;
    private double errorStdDevPx;
    // performance
    private double frameSpeedMs = 0;
    private double exposureTimeMs = 0;
    private double avgLatencyMs = 0;
    private double latencyStdDevMs = 0;
    // util
    private List<DMatrix3> viewplanes = new ArrayList<>();

    /** Default constructor which is the same as {@link #PERFECT_90DEG} */
    public SimCameraProperties() {
        setCalibration(960, 720, Rotation2d.fromDegrees(90));
    }

    /**
     * Reads camera properties from a photonvision <code>config.json</code> file. This is only the
     * resolution, camera intrinsics, distortion coefficients, and average/std. dev. pixel error.
     * Other camera properties must be set.
     *
     * @param path Path to the <code>config.json</code> file
     * @param width The width of the desired resolution in the JSON
     * @param height The height of the desired resolution in the JSON
     * @throws IOException If reading the JSON fails, either from an invalid path or a missing/invalid
     *     calibrated resolution.
     */
    public SimCameraProperties(String path, int width, int height) throws IOException {
        this(Path.of(path), width, height);
    }

    /**
     * Reads camera properties from a photonvision <code>config.json</code> file. This is only the
     * resolution, camera intrinsics, distortion coefficients, and average/std. dev. pixel error.
     * Other camera properties must be set.
     *
     * @param path Path to the <code>config.json</code> file
     * @param width The width of the desired resolution in the JSON
     * @param height The height of the desired resolution in the JSON
     * @throws IOException If reading the JSON fails, either from an invalid path or a missing/invalid
     *     calibrated resolution.
     */
    public SimCameraProperties(Path path, int width, int height) throws IOException {
        var mapper = new ObjectMapper();
        var json = mapper.readTree(path.toFile());
        json = json.get("calibrations");
        boolean success = false;
        try {
            for (int i = 0; i < json.size() && !success; i++) {
                // check if this calibration entry is our desired resolution
                var calib = json.get(i);
                int jsonWidth = calib.get("resolution").get("width").asInt();
                int jsonHeight = calib.get("resolution").get("height").asInt();
                if (jsonWidth != width || jsonHeight != height) continue;
                // get the relevant calibration values
                var jsonIntrinsicsNode = calib.get("cameraIntrinsics").get("data");
                double[] jsonIntrinsics = new double[jsonIntrinsicsNode.size()];
                for (int j = 0; j < jsonIntrinsicsNode.size(); j++) {
                    jsonIntrinsics[j] = jsonIntrinsicsNode.get(j).asDouble();
                }
                var jsonDistortNode = calib.get("cameraExtrinsics").get("data");
                double[] jsonDistortion = new double[jsonDistortNode.size()];
                for (int j = 0; j < jsonDistortNode.size(); j++) {
                    jsonDistortion[j] = jsonDistortNode.get(j).asDouble();
                }
                var jsonViewErrors = calib.get("perViewErrors");
                double jsonAvgError = 0;
                for (int j = 0; j < jsonViewErrors.size(); j++) {
                    jsonAvgError += jsonViewErrors.get(j).asDouble();
                }
                jsonAvgError /= jsonViewErrors.size();
                double jsonErrorStdDev = calib.get("standardDeviation").asDouble();
                // assign the read JSON values to this CameraProperties
                setCalibration(
                        jsonWidth,
                        jsonHeight,
                        Matrix.mat(Nat.N3(), Nat.N3()).fill(jsonIntrinsics),
                        Matrix.mat(Nat.N5(), Nat.N1()).fill(jsonDistortion));
                setCalibError(jsonAvgError, jsonErrorStdDev);
                success = true;
            }
        } catch (Exception e) {
            throw new IOException("Invalid calibration JSON");
        }
        if (!success) throw new IOException("Requested resolution not found in calibration");
    }

    public void setRandomSeed(long seed) {
        rand.setSeed(seed);
    }

    public void setCalibration(int resWidth, int resHeight, Rotation2d fovDiag) {
        double s = Math.sqrt(resWidth * resWidth + resHeight * resHeight);
        var fovWidth = new Rotation2d(fovDiag.getRadians() * (resWidth / s));
        var fovHeight = new Rotation2d(fovDiag.getRadians() * (resHeight / s));

        double maxFovDeg = Math.max(fovWidth.getDegrees(), fovHeight.getDegrees());
        if (maxFovDeg > 179) {
            double scale = 179.0 / maxFovDeg;
            fovWidth = new Rotation2d(fovWidth.getRadians() * scale);
            fovHeight = new Rotation2d(fovHeight.getRadians() * scale);
            DriverStation.reportError(
                    "Requested FOV width/height too large! Scaling below 180 degrees...", false);
        }

        // assume no distortion
        var distCoeff = VecBuilder.fill(0, 0, 0, 0, 0);

        // assume centered principal point (pixels)
        double cx = resWidth / 2.0;
        double cy = resHeight / 2.0;

        // use given fov to determine focal point (pixels)
        double fx = cx / Math.tan(fovWidth.getRadians() / 2.0);
        double fy = cy / Math.tan(fovHeight.getRadians() / 2.0);

        // create camera intrinsics matrix
        var camIntrinsics = Matrix.mat(Nat.N3(), Nat.N3()).fill(fx, 0, cx, 0, fy, cy, 0, 0, 1);
        setCalibration(resWidth, resHeight, camIntrinsics, distCoeff);
    }

    public void setCalibration(
            int resWidth, int resHeight, Matrix<N3, N3> camIntrinsics, Matrix<N5, N1> distCoeffs) {
        this.resWidth = resWidth;
        this.resHeight = resHeight;
        this.camIntrinsics = camIntrinsics;
        this.distCoeffs = distCoeffs;

        // left, right, up, and down view planes
        var p = new Translation3d[]{
            new Translation3d(1, new Rotation3d(0, 0, getPixelYaw(0).plus(new Rotation2d(-Math.PI/2)).getRadians())),
            new Translation3d(1, new Rotation3d(0, 0, getPixelYaw(resWidth).plus(new Rotation2d(Math.PI/2)).getRadians())),
            new Translation3d(1, new Rotation3d(0, getPixelPitch(0).plus(new Rotation2d(Math.PI/2)).getRadians(), 0)),
            new Translation3d(1, new Rotation3d(0, getPixelPitch(resHeight).plus(new Rotation2d(-Math.PI/2)).getRadians(), 0))
        };
        viewplanes.clear();
        for(int i = 0; i < p.length; i++) {
            viewplanes.add(new DMatrix3(p[i].getX(), p[i].getY(), p[i].getZ()));
        }
    }

    public void setCalibError(double avgErrorPx, double errorStdDevPx) {
        this.avgErrorPx = avgErrorPx;
        this.errorStdDevPx = errorStdDevPx;
    }

    /**
     * @param fps The average frames per second the camera should process at. <b>Exposure time limits
     *     FPS if set!</b>
     */
    public void setFPS(double fps) {
        frameSpeedMs = Math.max(1000.0 / fps, exposureTimeMs);
    }

    /**
     * @param exposureTimeMs The amount of time the "shutter" is open for one frame. Affects motion
     *     blur. <b>Frame speed(from FPS) is limited to this!</b>
     */
    public void setExposureTimeMs(double exposureTimeMs) {
        this.exposureTimeMs = exposureTimeMs;
        frameSpeedMs = Math.max(frameSpeedMs, exposureTimeMs);
    }

    /**
     * @param avgLatencyMs The average latency (from image capture to data published) in milliseconds
     *     a frame should have
     */
    public void setAvgLatencyMs(double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    /**
     * @param latencyStdDevMs The standard deviation in milliseconds of the latency
     */
    public void setLatencyStdDevMs(double latencyStdDevMs) {
        this.latencyStdDevMs = latencyStdDevMs;
    }

    public int getResWidth() {
        return resWidth;
    }

    public int getResHeight() {
        return resHeight;
    }

    public int getResArea() {
        return resWidth * resHeight;
    }

    /** Width:height */
    public double getAspectRatio() {
        return (double) resWidth / resHeight;
    }

    public Matrix<N3, N3> getIntrinsics() {
        return camIntrinsics.copy();
    }

    public Vector<N5> getDistCoeffs() {
        return new Vector<>(distCoeffs);
    }

    public double getFPS() {
        return 1000.0 / frameSpeedMs;
    }

    public double getFrameSpeedMs() {
        return frameSpeedMs;
    }

    public double getExposureTimeMs() {
        return exposureTimeMs;
    }

    public double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public double getLatencyStdDevMs() {
        return latencyStdDevMs;
    }

    public SimCameraProperties copy() {
        var newProp = new SimCameraProperties();
        newProp.setCalibration(resWidth, resHeight, camIntrinsics, distCoeffs);
        newProp.setCalibError(avgErrorPx, errorStdDevPx);
        newProp.setFPS(getFPS());
        newProp.setExposureTimeMs(exposureTimeMs);
        newProp.setAvgLatencyMs(avgLatencyMs);
        newProp.setLatencyStdDevMs(latencyStdDevMs);
        return newProp;
    }

    public List<TargetCorner> undistort(List<TargetCorner> points) {
        return OpenCVHelp.undistortPoints(camIntrinsics, distCoeffs, points);
    }

    /**
     * The percentage(0 - 100) of this camera's resolution the contour takes up in pixels of the
     * image.
     *
     * @param corners Corners of the contour
     */
    public double getContourAreaPercent(List<TargetCorner> corners) {
        return Imgproc.contourArea(OpenCVHelp.getConvexHull(corners)) / getResArea() * 100;
    }

    /** The yaw from the principal point of this camera to the pixel x value. Positive values left. */
    public Rotation2d getPixelYaw(double pixelX) {
        double fx = camIntrinsics.get(0, 0);
        // account for principal point not being centered
        double cx = camIntrinsics.get(0, 2);
        double xOffset = cx - pixelX;
        return new Rotation2d(fx, xOffset);
    }

    /**
     * The pitch from the principal point of this camera to the pixel y value. Pitch is positive down.
     *
     * <p>Note that this angle is naively computed and may be incorrect. See {@link
     * #getCorrectedPixelRot(TargetCorner)}.
     */
    public Rotation2d getPixelPitch(double pixelY) {
        double fy = camIntrinsics.get(1, 1);
        // account for principal point not being centered
        double cy = camIntrinsics.get(1, 2);
        double yOffset = cy - pixelY;
        return new Rotation2d(fy, -yOffset);
    }

    /**
     * Finds the yaw and pitch to the given image point. Yaw is positive left, and pitch is positive
     * down.
     *
     * <p>Note that pitch is naively computed and may be incorrect. See {@link
     * #getCorrectedPixelRot(TargetCorner)}.
     */
    public Rotation3d getPixelRot(TargetCorner point) {
        return new Rotation3d(
                0, getPixelPitch(point.y).getRadians(), getPixelYaw(point.x).getRadians());
    }

    /**
     * Gives the yaw and pitch of the line intersecting the camera lens and the given pixel
     * coordinates on the sensor. Yaw is positive left, and pitch positive down.
     *
     * <p>The pitch traditionally calculated from pixel offsets do not correctly account for non-zero
     * values of yaw because of perspective distortion (not to be confused with lens distortion)-- for
     * example, the pitch angle is naively calculated as:
     *
     * <pre>pitch = arctan(pixel y offset / focal length y)</pre>
     *
     * However, using focal length as a side of the associated right triangle is not correct when the
     * pixel x value is not 0, because the distance from this pixel (projected on the x-axis) to the
     * camera lens increases. Projecting a line back out of the camera with these naive angles will
     * not intersect the 3d point that was originally projected into this 2d pixel. Instead, this
     * length should be:
     *
     * <pre>focal length y ⟶ (focal length y / cos(arctan(pixel x offset / focal length x)))</pre>
     *
     * @return Rotation3d with yaw and pitch of the line projected out of the camera from the given
     *     pixel (roll is zero).
     */
    public Rotation3d getCorrectedPixelRot(TargetCorner point) {
        double fx = camIntrinsics.get(0, 0);
        double cx = camIntrinsics.get(0, 2);
        double xOffset = cx - point.x;

        double fy = camIntrinsics.get(1, 1);
        double cy = camIntrinsics.get(1, 2);
        double yOffset = cy - point.y;

        // calculate yaw normally
        var yaw = new Rotation2d(fx, xOffset);
        // correct pitch based on yaw
        var pitch = new Rotation2d(fy / Math.cos(Math.atan(xOffset / fx)), -yOffset);

        return new Rotation3d(0, pitch.getRadians(), yaw.getRadians());
    }

    public Rotation2d getHorizFOV() {
        // sum of FOV left and right principal point
        var left = getPixelYaw(0);
        var right = getPixelYaw(resWidth);
        return left.minus(right);
    }

    public Rotation2d getVertFOV() {
        // sum of FOV above and below principal point
        var above = getPixelPitch(0);
        var below = getPixelPitch(resHeight);
        return below.minus(above);
    }

    public Rotation2d getDiagFOV() {
        return new Rotation2d(Math.hypot(getHorizFOV().getRadians(), getVertFOV().getRadians()));
    }

    /**
     * Determines where the line segment defined by the two given translations intersects the camera's
     * frustum/field-of-vision, if at all.
     * 
     * <p>The line is parametrized so any of its points <code>p = t * (b - a) + a</code>.
     * This method returns these values of t, minimum first, defining the region of the line segment
     * which is visible in the frustum. If both ends of the line segment are visible, this simply
     * returns {0, 1}. If, for example, point b is visible while a is not, and half of the line segment
     * is inside the camera frustum, {0.5, 1} would be returned. 
     * 
     * @param camRt The change in basis from world coordinates to camera coordinates. See
     *     {@link RotTrlTransform3d#makeRelativeTo(Pose3d)}.
     * @param a The initial translation of the line
     * @param b The final translation of the line
     * @return A Pair of Doubles. The values may be null:
     *     <ul>
     *       <li> {Double, Double} : Two parametrized values(t), minimum first, representing which segment
     *       of the line is visible in the camera frustum.
     *       <li> {Double, null} : One value(t) representing a single intersection point. For example, the
     *       line only intersects the intersection of two adjacent viewplanes.
     *       <li> {null, null} : No values. The line segment is not visible in the camera frustum.
     *     </ul>
     */
    public Pair<Double, Double> getVisibleLine(RotTrlTransform3d camRt, Translation3d a, Translation3d b) {
        // translations relative to the camera
        var rela = camRt.apply(a);
        var relb = camRt.apply(b);

        // check if both ends are behind camera
        if(rela.getX() <= 0 && relb.getX() <= 0) return new Pair<>(null, null);

        var av = new DMatrix3(rela.getX(), rela.getY(), rela.getZ());
        var bv = new DMatrix3(relb.getX(), relb.getY(), relb.getZ());
        // a to b
        var abv = new DMatrix3();
        CommonOps_DDF3.subtract(bv, av, abv);

        // check if the ends of the line segment are visible
        boolean aVisible = true;
        boolean bVisible = true;
        for(int i = 0; i < viewplanes.size(); i++) {
            var normal = viewplanes.get(i);
            double aVisibility = CommonOps_DDF3.dot(av, normal);
            if(aVisibility < 0) aVisible = false;
            double bVisibility = CommonOps_DDF3.dot(bv, normal);
            if(bVisibility < 0) bVisible = false;
            // both ends are outside at least one of the same viewplane
            if(aVisibility <= 0 && bVisibility <= 0) return new Pair<>(null, null);
        }
        // both ends are inside frustum
        if(aVisible && bVisible) return new Pair<>(Double.valueOf(0), Double.valueOf(1));
        
        // parametrized (t=0 at a, t=1 at b) intersections with viewplanes
        double[] intersections = {
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN
        };
        // intersection points
        List<DMatrix3> ipts = new ArrayList<>();
        for(double val : intersections) ipts.add(null);

        // find intersections
        for(int i = 0; i < viewplanes.size(); i++) {
            var normal = viewplanes.get(i);

            // we want to know the value of t when the line intercepts this plane
            // parametrized: v = t * ab + a, where v lies on the plane
            // we can find the projection of a onto the plane normal
            // a_projn = normal.times(av.dot(normal) / normal.dot(normal));
            var a_projn = new DMatrix3();
            CommonOps_DDF3.scale(CommonOps_DDF3.dot(av, normal) / CommonOps_DDF3.dot(normal, normal), normal, a_projn);
            // this projection lets us determine the scalar multiple t of ab where
            // (t * ab + a) is a vector which lies on the plane
            if(Math.abs(CommonOps_DDF3.dot(abv, a_projn)) < 1e-5) continue; // line is parallel to plane
            intersections[i] = CommonOps_DDF3.dot(a_projn, a_projn) / -CommonOps_DDF3.dot(abv, a_projn);

            // vector a to the viewplane
            var apv = new DMatrix3();
            CommonOps_DDF3.scale(intersections[i], abv, apv);
            // av + apv = intersection point
            var intersectpt = new DMatrix3();
            CommonOps_DDF3.add(av, apv, intersectpt);
            ipts.set(i, intersectpt);

            // discard intersections outside the camera frustum
            for(int j = 1; j < viewplanes.size(); j++) {
                int oi = (i+j) % viewplanes.size();
                var onormal = viewplanes.get(oi);
                // if the dot of the intersection point with any plane normal is negative, it is outside
                if(CommonOps_DDF3.dot(intersectpt, onormal) < 0) {
                    intersections[i] = Double.NaN;
                    ipts.set(i, null);
                    break;
                }
            }

            // discard duplicate intersections
            if(ipts.get(i) == null) continue;
            for(int j = i-1; j >= 0; j--) {
                var oipt = ipts.get(j);
                if(oipt == null) continue;
                var diff = new DMatrix3();
                CommonOps_DDF3.subtract(oipt, intersectpt, diff);
                if(CommonOps_DDF3.elementMaxAbs(diff) < 1e-4) {
                    intersections[i] = Double.NaN;
                    ipts.set(i, null);
                    break;
                }
            }
        }

        // determine visible segment (minimum and maximum t)
        double inter1 = Double.NaN;
        double inter2 = Double.NaN;
        for(double inter : intersections) {
            if(!Double.isNaN(inter)) {
                if(Double.isNaN(inter1)) inter1 = inter;
                else inter2 = inter;
            }
        }

        // two viewplane intersections
        if(!Double.isNaN(inter2)) {
            double max = Math.max(inter1, inter2);
            double min = Math.min(inter1, inter2);
            if(aVisible) min = 0;
            if(bVisible) max = 1;
            return new Pair<>(
                min,
                max
            );
        }
        // one viewplane intersection
        else if(!Double.isNaN(inter1)) {
            if(aVisible) return new Pair<>(Double.valueOf(0), inter1);
            if(bVisible) return new Pair<>(inter1, Double.valueOf(1));
            return new Pair<>(inter1, null);
        }
        // no intersections
        else return new Pair<>(null, null);
    }

    /**
     * Returns these pixel points as fractions of a 1x1 square image. This means the camera's aspect
     * ratio and resolution will be used, and the points' x and y may not reach all portions(e.g. a
     * wide aspect ratio means some of the top and bottom of the square image is unreachable).
     *
     * @param points Pixel points on this camera's image
     * @return Points mapped to an image of 1x1 resolution
     */
    public List<TargetCorner> getPixelFraction(List<TargetCorner> points) {
        double resLarge = getAspectRatio() > 1 ? resWidth : resHeight;

        return points.stream()
                .map(
                        p -> {
                            // offset to account for aspect ratio
                            return new TargetCorner(
                                    (p.x + (resLarge - resWidth) / 2.0) / resLarge,
                                    (p.y + (resLarge - resHeight) / 2.0) / resLarge);
                        })
                .collect(Collectors.toList());
    }

    /** Returns these points after applying this camera's estimated noise. */
    public List<TargetCorner> estPixelNoise(List<TargetCorner> points) {
        if (avgErrorPx == 0 && errorStdDevPx == 0) return points;

        return points.stream()
                .map(
                        p -> {
                            // error pixels in random direction
                            double error = avgErrorPx + rand.nextGaussian() * errorStdDevPx;
                            double errorAngle = rand.nextDouble() * 2 * Math.PI - Math.PI;
                            return new TargetCorner(
                                    p.x + error * Math.cos(errorAngle), p.y + error * Math.sin(errorAngle));
                        })
                .collect(Collectors.toList());
    }

    /**
     * @return Noisy estimation of a frame's processing latency in milliseconds
     */
    public double estLatencyMs() {
        return Math.max(avgLatencyMs + rand.nextGaussian() * latencyStdDevMs, 0);
    }

    /**
     * @return Estimate how long until the next frame should be processed in milliseconds
     */
    public double estMsUntilNextFrame() {
        // exceptional processing latency blocks the next frame
        return frameSpeedMs + Math.max(0, estLatencyMs() - frameSpeedMs);
    }

    // pre-calibrated example cameras

    /** 960x720 resolution, 90 degree FOV, "perfect" lagless camera */
    public static SimCameraProperties PERFECT_90DEG() {
        return new SimCameraProperties();
    }

    public static SimCameraProperties PI4_LIFECAM_320_240() {
        var prop = new SimCameraProperties();
        prop.setCalibration(
                320,
                240,
                Matrix.mat(Nat.N3(), Nat.N3())
                        .fill( // intrinsic
                                328.2733242048587,
                                0.0,
                                164.8190261141906,
                                0.0,
                                318.0609794305216,
                                123.8633838438093,
                                0.0,
                                0.0,
                                1.0),
                VecBuilder.fill( // distort
                        0.09957946553445934,
                        -0.9166265114485799,
                        0.0019519890627236526,
                        -0.0036071725380870333,
                        1.5627234622420942));
        prop.setCalibError(0.21, 0.0124);
        prop.setFPS(30);
        prop.setAvgLatencyMs(30);
        prop.setLatencyStdDevMs(10);
        return prop;
    }

    public static SimCameraProperties PI4_LIFECAM_640_480() {
        var prop = new SimCameraProperties();
        prop.setCalibration(
                640,
                480,
                Matrix.mat(Nat.N3(), Nat.N3())
                        .fill( // intrinsic
                                669.1428078983059,
                                0.0,
                                322.53377249329213,
                                0.0,
                                646.9843137061716,
                                241.26567383784163,
                                0.0,
                                0.0,
                                1.0),
                VecBuilder.fill( // distort
                        0.12788470750464645,
                        -1.2350335805796528,
                        0.0024990767286192732,
                        -0.0026958287600230705,
                        2.2951386729115537));
        prop.setCalibError(0.26, 0.046);
        prop.setFPS(15);
        prop.setAvgLatencyMs(65);
        prop.setLatencyStdDevMs(15);
        return prop;
    }

    public static SimCameraProperties LL2_640_480() {
        var prop = new SimCameraProperties();
        prop.setCalibration(
                640,
                480,
                Matrix.mat(Nat.N3(), Nat.N3())
                        .fill( // intrinsic
                                511.22843367007755,
                                0.0,
                                323.62049380211096,
                                0.0,
                                514.5452336723849,
                                261.8827920543568,
                                0.0,
                                0.0,
                                1.0),
                VecBuilder.fill( // distort
                        0.1917469998873756,
                        -0.5142936883324216,
                        0.012461562046896614,
                        0.0014084973492408186,
                        0.35160648971214437));
        prop.setCalibError(0.25, 0.05);
        prop.setFPS(15);
        prop.setAvgLatencyMs(35);
        prop.setLatencyStdDevMs(8);
        return prop;
    }

    public static SimCameraProperties LL2_960_720() {
        var prop = new SimCameraProperties();
        prop.setCalibration(
                960,
                720,
                Matrix.mat(Nat.N3(), Nat.N3())
                        .fill( // intrinsic
                                769.6873145148892,
                                0.0,
                                486.1096609458122,
                                0.0,
                                773.8164483705323,
                                384.66071662358354,
                                0.0,
                                0.0,
                                1.0),
                VecBuilder.fill( // distort
                        0.189462064814501,
                        -0.49903003669627627,
                        0.007468423590519429,
                        0.002496885298683693,
                        0.3443122090208624));
        prop.setCalibError(0.35, 0.10);
        prop.setFPS(10);
        prop.setAvgLatencyMs(50);
        prop.setLatencyStdDevMs(15);
        return prop;
    }

    public static SimCameraProperties LL2_1280_720() {
        var prop = new SimCameraProperties();
        prop.setCalibration(
                1280,
                720,
                Matrix.mat(Nat.N3(), Nat.N3())
                        .fill( // intrinsic
                                1011.3749416937393,
                                0.0,
                                645.4955139388737,
                                0.0,
                                1008.5391755084075,
                                508.32877656020196,
                                0.0,
                                0.0,
                                1.0),
                VecBuilder.fill( // distort
                        0.13730101577061535,
                        -0.2904345656989261,
                        8.32475714507539E-4,
                        -3.694397782014239E-4,
                        0.09487962227027584));
        prop.setCalibError(0.37, 0.06);
        prop.setFPS(7);
        prop.setAvgLatencyMs(60);
        prop.setLatencyStdDevMs(20);
        return prop;
    }
}
