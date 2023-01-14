/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
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

package org.photonvision;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.photonvision.targeting.TargetCorner;
import org.photonvision.util.OpenCVHelp;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.numbers.*;
import edu.wpi.first.wpilibj.DriverStation;

    /**
     * Calibration and performance values for this camera.
     * 
     * <p>The resolution will affect the accuracy of projected(3d to 2d) target corners and
     * similarly the severity of image noise on estimation(2d to 3d).</p>
     * 
     * <p>The camera intrinsics and distortion coefficients describe the results of calibration,
     * and how to map between 3d field points and 2d image points.</p>
     * 
     * <p>The performance values (framerate/exposure time, latency) determine how often results
     * should be updated and with how much latency in simulation. High exposure time causes motion
     * blur which can inhibit target detection while moving. Note that latency estimation does not
     * account for network latency and the latency reported will always be perfect.</p>
     */
public class CameraProperties {
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

    /**
     * Default constructor which is the same as {@link #PERFECT_90DEG}
     */
    public CameraProperties() {
        setCalibration(960, 720, Rotation2d.fromDegrees(90));
    };
    /**
     * Reads camera properties from a photonvision <code>config.json</code> file.
     * This is only the resolution, camera intrinsics, distortion coefficients,
     * and average/std. dev. pixel error. Other camera properties must be set.
     * 
     * @param path Path to the <code>config.json</code> file
     * @param width The width of the desired resolution in the JSON
     * @param height The height of the desired resolution in the JSON
     * @throws IOException If reading the JSON fails, either from an invalid path
     *     or a missing/invalid calibrated resolution.
     */
    public CameraProperties(String path, int width, int height) throws IOException {
        this(Path.of(path), width, height);
    }
    /**
     * Reads camera properties from a photonvision <code>config.json</code> file.
     * This is only the resolution, camera intrinsics, distortion coefficients,
     * and average/std. dev. pixel error. Other camera properties must be set.
     * 
     * @param path Path to the <code>config.json</code> file
     * @param width The width of the desired resolution in the JSON
     * @param height The height of the desired resolution in the JSON
     * @throws IOException If reading the JSON fails, either from an invalid path
     *     or a missing/invalid calibrated resolution.
     */
    public CameraProperties(Path path, int width, int height) throws IOException {
        var mapper = new ObjectMapper();
        var json = mapper.readTree(path.toFile());
        json = json.get("calibrations");
        boolean success = false;
        try {
            for(int i = 0; i < json.size() && !success; i++) {
                // check if this calibration entry is our desired resolution
                var calib = json.get(i);
                int jsonWidth = calib.get("resolution").get("width").asInt();
                int jsonHeight = calib.get("resolution").get("height").asInt();
                if(jsonWidth != width || jsonHeight != height) continue;
                // get the relevant calibration values
                var jsonIntrinsicsNode = calib.get("cameraIntrinsics").get("data");
                double[] jsonIntrinsics = new double[jsonIntrinsicsNode.size()];
                for(int j = 0; j < jsonIntrinsicsNode.size(); j++) {
                    jsonIntrinsics[j] = jsonIntrinsicsNode.get(j).asDouble();
                }
                var jsonDistortNode = calib.get("cameraExtrinsics").get("data");
                double[] jsonDistortion = new double[jsonDistortNode.size()];
                for(int j = 0; j < jsonDistortNode.size(); j++) {
                    jsonDistortion[j] = jsonDistortNode.get(j).asDouble();
                }
                var jsonViewErrors = calib.get("perViewErrors");
                double jsonAvgError = 0;
                for(int j = 0; j < jsonViewErrors.size(); j++) {
                    jsonAvgError += jsonViewErrors.get(j).asDouble();
                }
                jsonAvgError /= jsonViewErrors.size();
                double jsonErrorStdDev = calib.get("standardDeviation").asDouble();
                // assign the read JSON values to this CameraProperties
                setCalibration(
                    jsonWidth, jsonHeight,
                    Matrix.mat(Nat.N3(), Nat.N3()).fill(jsonIntrinsics),
                    Matrix.mat(Nat.N5(), Nat.N1()).fill(jsonDistortion)
                );
                setCalibError(jsonAvgError, jsonErrorStdDev);
                success = true;
            }
        } catch(Exception e) {
            throw new IOException("Invalid calibration JSON");
        }
        if(!success) throw new IOException("Requested resolution not found in calibration");
    }

    public void setRandomSeed(long seed) {
        rand.setSeed(seed);
    }

    public void setCalibration(int resWidth, int resHeight, Rotation2d fovDiag) {
        var fovWidth = new Rotation2d(
            fovDiag.getRadians() * (resWidth / Math.sqrt(resWidth*resWidth + resHeight*resHeight))
        );
        var fovHeight = new Rotation2d(
            fovDiag.getRadians() * (resHeight / Math.sqrt(resWidth*resWidth + resHeight*resHeight))
        );
        double maxFovDeg = Math.max(fovWidth.getDegrees(), fovHeight.getDegrees());
        if(maxFovDeg > 179) {
            double scale = 179.0 / maxFovDeg;
            fovWidth = new Rotation2d(fovWidth.getRadians() * scale);
            fovHeight = new Rotation2d(fovHeight.getRadians() * scale);
            DriverStation.reportError(
                    "Requested FOV width/height too large! Scaling below 180 degrees...", false);
        }
        // assume no distortion
        var distCoeff = VecBuilder.fill(0,0,0,0,0);
        // assume centered principal point (pixels)
        double cx = resWidth/2.0;
        double cy = resHeight/2.0;
        // use given fov to determine focal point (pixels)
        double fx = cx / Math.tan(fovWidth.getRadians()/2.0);
        double fy = cy / Math.tan(fovHeight.getRadians()/2.0);
        // create camera intrinsics matrix
        var camIntrinsics = Matrix.mat(Nat.N3(), Nat.N3()).fill(
            fx, 0, cx,
            0, fy, cy,
            0,  0, 1
        );
        setCalibration(resWidth, resHeight, camIntrinsics, distCoeff);
    }
    public void setCalibration(int resWidth, int resHeight,
            Matrix<N3, N3> camIntrinsics, Matrix<N5, N1> distCoeffs) {
        this.resWidth = resWidth;
        this.resHeight = resHeight;
        this.camIntrinsics = camIntrinsics;
        this.distCoeffs = distCoeffs;
    }
    public void setCameraIntrinsics(Matrix<N3, N3> camIntrinsics) {
        this.camIntrinsics = camIntrinsics;
    }
    public void setDistortionCoeffs(Matrix<N5, N1> distCoeffs) {
        this.distCoeffs = distCoeffs;
    }
    public void setCalibError(double avgErrorPx, double errorStdDevPx) {
        this.avgErrorPx = avgErrorPx;
        this.errorStdDevPx = errorStdDevPx;
    }
    /**
     * @param fps The average frames per second the camera should process at.
     *     <b>Exposure time limits FPS if set!</b>
     */
    public void setFPS(double fps) {
        this.frameSpeedMs = Math.max(1000.0 / fps, exposureTimeMs);
    }
    /**
     * @param exposureTimeMs The amount of time the "shutter" is open for one frame.
     *     Affects motion blur. <b>Frame speed(from FPS) is limited to this!</b>
     */
    public void setExposureTimeMs(double exposureTimeMs) {
        this.exposureTimeMs = exposureTimeMs;
        frameSpeedMs = Math.max(frameSpeedMs, exposureTimeMs);
    }
    /**
     * @param avgLatencyMs The average latency (from image capture to data published) in
     *     milliseconds a frame should have
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
        return resWidth*resHeight;
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

    public CameraProperties copy() {
        var newProp = new CameraProperties();
        newProp.setCalibration(resWidth, resHeight, camIntrinsics, distCoeffs);
        newProp.setCalibError(avgErrorPx, errorStdDevPx);
        newProp.setFPS(getFPS());
        newProp.setExposureTimeMs(exposureTimeMs);
        newProp.setAvgLatencyMs(avgLatencyMs);
        newProp.setLatencyStdDevMs(latencyStdDevMs);
        return newProp;
    }

    public List<TargetCorner> undistort(List<TargetCorner> points) {
        return OpenCVHelp.undistortPoints(this, points);
    }

    /**
     * The percentage(0 - 100) of this camera's resolution the contour takes up in pixels
     * of the image.
     * 
     * @param corners Corners of the contour
     */
    public double getContourAreaPercent(List<TargetCorner> corners) {
        return OpenCVHelp.getContourAreaPx(corners) / getResArea() * 100;
    }

    /**
     * The yaw from the principal point of this camera to the pixel x value.
     * Positive values left.
     */
    public Rotation2d getPixelYaw(double pixelX) {
        double fx = camIntrinsics.get(0, 0);
        // account for principal point not being centered
        double cx = camIntrinsics.get(0, 2);
        double xOffset = cx - pixelX;
        return new Rotation2d(
            fx,
            xOffset
        );
    }
    /**
     * The pitch from the principal point of this camera to the pixel y value.
     * Pitch is positive down.
     * 
     * <p>Note that this angle is naively computed and may be incorrect.
     * See {@link #getCorrectedPixelRot(TargetCorner)}.</p>
     */
    public Rotation2d getPixelPitch(double pixelY) {
        double fy = camIntrinsics.get(1, 1);
        // account for principal point not being centered
        double cy = camIntrinsics.get(1, 2);
        double yOffset = cy - pixelY;
        return new Rotation2d(
            fy,
            -yOffset
        );
    }
    /**
     * Finds the yaw and pitch to the given image point.
     * Yaw is positive left, and pitch is positive down.
     * 
     * <p>Note that pitch is naively computed and may be incorrect.
     * See {@link #getCorrectedPixelRot(TargetCorner)}.</p>
     */
    public Rotation3d getPixelRot(TargetCorner point) {
        return new Rotation3d(
            0,
            getPixelPitch(point.y).getRadians(),
            getPixelYaw(point.x).getRadians()
        );
    }
    /**
     * Gives the yaw and pitch of the line intersecting the camera lens and the given pixel coordinates
     * on the sensor. Yaw is positive left, and pitch positive down.
     * 
     * <p>
     * The pitch traditionally calculated from pixel offsets do not correctly account
     * for non-zero values of yaw because of perspective distortion (not to be confused
     * with lens distortion)-- for example, the pitch angle is naively calculated as:
     * 
     * <pre>pitch = arctan(pixel y offset / focal length y)</pre>
     * 
     * However, using focal length as a side of the associated right triangle is not correct when
     * the pixel x value is not 0, because the distance from this pixel (projected on the x-axis)
     * to the camera lens increases. Projecting a line back out of the camera with these naive angles
     * will not intersect the 3d point that was originally projected into this 2d pixel.
     * Instead, this length should be:
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
        var yaw = new Rotation2d(
            fx,
            xOffset
        );
        // correct pitch based on yaw
        var pitch = new Rotation2d(
            fy / Math.cos(Math.atan(xOffset / fx)),
            -yOffset
        );

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
    /** Width:height */
    public double getAspectRatio() {
        return (double)resWidth / resHeight;
    }
    /**
     * Returns these pixel points as fractions of a 1x1 square image.
     * This means the camera's aspect ratio and resolution will be used, and the
     * points' x and y may not reach all portions(e.g. a wide aspect ratio means
     * some of the top and bottom of the square image is unreachable).
     * 
     * @param points Pixel points on this camera's image
     * @return Points mapped to an image of 1x1 resolution
     */
    public List<TargetCorner> getPixelFraction(List<TargetCorner> points) {
        double resLarge = getAspectRatio() > 1 ? resWidth : resHeight;

        return points.stream()
            .map(p -> {
                // offset to account for aspect ratio
                return new TargetCorner(
                    (p.x + (resLarge-resWidth)/2.0) / resLarge,
                    (p.y + (resLarge-resHeight)/2.0) / resLarge
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Returns these points after applying this camera's estimated noise.
     */
    public List<TargetCorner> estPixelNoise(List<TargetCorner> points) {
        if(avgErrorPx == 0 && errorStdDevPx == 0) return points;

        return points.stream()
            .map(p -> {
                // error pixels in random direction
                double error = avgErrorPx + rand.nextGaussian()*errorStdDevPx;
                double errorAngle = rand.nextDouble() * 2 * Math.PI - Math.PI;
                return new TargetCorner(
                    p.x + error*Math.cos(errorAngle),
                    p.y + error*Math.sin(errorAngle)
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * @return Noisy estimation of a frame's processing latency in milliseconds
     */
    public double estLatencyMs() {
        return Math.max(avgLatencyMs + rand.nextGaussian()*latencyStdDevMs, 0);
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
    public static CameraProperties PERFECT_90DEG() {
        return new CameraProperties();
    }
    public static CameraProperties PI4_PICAM2_480p() {
        var prop = new CameraProperties();
        prop.setCalibration(640, 480,
            Matrix.mat(Nat.N3(), Nat.N3()).fill( // intrinsic
                497.8204072694636,  0.0,                314.53659309737424,
                0.0,                481.6955284883231,  231.95042993880858,
                0.0,                0.0,                1.0
            ),
            VecBuilder.fill( // distort
                0.16990717177326176,
                -0.47305087536583684,
                -0.002992219989630736,
                -0.0016840919550094836,
                0.36623021008942863
            )
        );
        prop.setCalibError(0.25, 0.07);
        prop.setFPS(17);
        prop.setAvgLatencyMs(35);
        prop.setLatencyStdDevMs(8);
        return prop;
    }
}
