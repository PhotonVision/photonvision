package org.photonvision.estimation;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.numbers.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;

/**
 * Calibration and performance values for this camera.
 *
 * <p>The resolution will affect the accuracy of projected(3d->2d) target corners and similarly the
 * severity of image noise on estimation(2d->3d).
 *
 * <p>The camera intrinsics and distortion coefficients describe the results of calibration, and how
 * to map between 3d field points and 2d image points.
 *
 * <p>The performance values (framerate/exposure time, latency) determine how often results should
 * be updated and with how much latency in simulation. High exposure time causes motion blur which
 * can inhibit target detection while moving. Note that latency estimation does not account for
 * network latency and the latency reported will always be perfect.
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

    /** Default constructor which is the same as {@link #PERFECT_90DEG} */
    public CameraProperties() {
        setCalibration(960, 720, Rotation2d.fromDegrees(90));
    }
    ;

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
    public CameraProperties(String path, int width, int height) throws IOException {
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
    public CameraProperties(Path path, int width, int height) throws IOException {
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
        var fovWidth =
                new Rotation2d(
                        fovDiag.getRadians()
                                * (resWidth / Math.sqrt(resWidth * resWidth + resHeight * resHeight)));
        var fovHeight =
                new Rotation2d(
                        fovDiag.getRadians()
                                * (resHeight / Math.sqrt(resWidth * resWidth + resHeight * resHeight)));
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
     * @param fps The average frames per second the camera should process at. <b>Exposure time limits
     *     FPS if set!</b>
     */
    public void setFPS(double fps) {
        this.frameSpeedMs = Math.max(1000.0 / fps, exposureTimeMs);
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
     * @param avgLatencyMs The average latency (image capture -> data) in milliseconds a frame should
     *     have
     */
    public void setAvgLatencyMs(double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    /** @param latencyStdDevMs The standard deviation in milliseconds of the latency */
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
        return new CameraProperties();
    }

    /**
     * Undistorts a detected target's corner points, and returns a new PhotonTrackedTarget with
     * updated corners, yaw, pitch, skew, and area. Best/alt pose and ambiguity are unchanged.
     */
    public PhotonTrackedTarget undistort2dTarget(PhotonTrackedTarget target) {
        var undistortedCorners = undistort(target.getDetectedCorners());
        // find the 2d yaw/pitch
        var boundingCenterRot = getPixelRot(undistortedCorners);
        // find contour area
        double areaPercent = getContourAreaPercent(undistortedCorners);

        return new PhotonTrackedTarget(
                Math.toDegrees(boundingCenterRot.getZ()),
                -Math.toDegrees(boundingCenterRot.getY()),
                areaPercent,
                Math.toDegrees(boundingCenterRot.getX()),
                target.getFiducialId(),
                target.getBestCameraToTarget(),
                target.getAlternateCameraToTarget(),
                target.getPoseAmbiguity(),
                undistortedCorners,
                undistortedCorners);
    }

    public List<TargetCorner> undistort(List<TargetCorner> points) {
        return OpenCVHelp.undistortPoints(this, points);
    }

    /**
     * The percentage(0 - 100) of this camera's resolution the contour takes up in pixels of the
     * image.
     *
     * @param corners Corners of the contour
     */
    public double getContourAreaPercent(List<TargetCorner> corners) {
        return OpenCVHelp.getContourAreaPx(corners) / getResArea() * 100;
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
     */
    public Rotation2d getPixelPitch(double pixelY) {
        double fy = camIntrinsics.get(1, 1);
        // account for principal point not being centered
        double cy = camIntrinsics.get(1, 2);
        double yOffset = cy - pixelY;
        return new Rotation2d(fy, -yOffset);
    }

    /**
     * Undistorts these image points, and then finds the yaw and pitch to the center of the rectangle
     * bounding them. Yaw is positive left, and pitch is positive down.
     */
    public Rotation3d getUndistortedPixelRot(List<TargetCorner> points) {
        return getPixelRot(undistort(points));
    }

    /**
     * Finds the yaw and pitch to the center of the rectangle bounding the given image points. Yaw is
     * positive left, and pitch is positive down.
     */
    public Rotation3d getPixelRot(List<TargetCorner> points) {
        if (points == null || points.size() == 0) return new Rotation3d();

        var rect = OpenCVHelp.getMinAreaRect(points);
        return new Rotation3d(
                rect.angle,
                getPixelPitch(rect.center.y).getRadians(),
                getPixelYaw(rect.center.x).getRadians());
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
        return (double) resWidth / resHeight;
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

    /** @return Estimate of new points based on this camera's noise. */
    public List<TargetCorner> estPixelNoise(List<TargetCorner> points) {
        if (avgErrorPx == 0 && errorStdDevPx == 0) return points;

        return points.stream()
                .map(
                        p -> {
                            // error pixels in random direction
                            double error = rand.nextGaussian() * errorStdDevPx + avgErrorPx;
                            double errorAngle = rand.nextDouble() * Math.PI;
                            return new TargetCorner(
                                    p.x + error * Math.cos(errorAngle), p.y + error * Math.sin(errorAngle));
                        })
                .collect(Collectors.toList());
    }

    /** @return Noisy estimation of a frame's processing latency in milliseconds */
    public double estLatencyMs() {
        return Math.max((rand.nextGaussian() * latencyStdDevMs) + avgLatencyMs, 0);
    }

    /** @return Estimate how long until the next frame should be processed in milliseconds */
    public double estMsUntilNextFrame() {
        // exceptional processing latency blocks the next frame
        return frameSpeedMs + Math.max(0, estLatencyMs() - frameSpeedMs);
    }

    // pre-calibrated example cameras

    /** 960x720 resolution, 90 degree FOV, "perfect" lagless camera */
    public static final CameraProperties PERFECT_90DEG = new CameraProperties();

    public static final CameraProperties PI4_PICAM2_480p = new CameraProperties();

    public static final CameraProperties LL2_480p = new CameraProperties();
    public static final CameraProperties LL2_960_720p = new CameraProperties();
    public static final CameraProperties LL2_1280_720p = new CameraProperties();

    public static final CameraProperties LIFECAM_1280_720p = new CameraProperties();

    static {
        PI4_PICAM2_480p.setCalibration(
                640,
                480,
                Matrix.mat(Nat.N3(), Nat.N3())
                        .fill( // intrinsic
                                497.8204072694636,
                                0.0,
                                314.53659309737424,
                                0.0,
                                481.6955284883231,
                                231.95042993880858,
                                0.0,
                                0.0,
                                1.0),
                VecBuilder.fill( // distort
                        0.16990717177326176,
                        -0.47305087536583684,
                        -0.002992219989630736,
                        -0.0016840919550094836,
                        0.36623021008942863));
        PI4_PICAM2_480p.setCalibError(0.25, 0.07);
        PI4_PICAM2_480p.setFPS(17);
        PI4_PICAM2_480p.setAvgLatencyMs(35);
        PI4_PICAM2_480p.setLatencyStdDevMs(8);

        LL2_480p.setCalibration(
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
        LL2_480p.setCalibError(0.25, 0.05);
        LL2_480p.setFPS(15);
        LL2_480p.setAvgLatencyMs(35);
        LL2_480p.setLatencyStdDevMs(8);

        LL2_960_720p.setCalibration(
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
        LL2_960_720p.setCalibError(0.35, 0.10);
        LL2_960_720p.setFPS(10);
        LL2_960_720p.setAvgLatencyMs(50);
        LL2_960_720p.setLatencyStdDevMs(15);

        LL2_1280_720p.setCalibration(
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
        LL2_1280_720p.setCalibError(0.37, 0.06);
        LL2_1280_720p.setFPS(7);
        LL2_1280_720p.setAvgLatencyMs(60);
        LL2_1280_720p.setLatencyStdDevMs(20);

        LIFECAM_1280_720p.setCalibration(
                1280,
                720,
                Matrix.mat(Nat.N3(), Nat.N3())
                        .fill( // intrinsic
                                1150.2178512391217,
                                0,
                                620.9005800934796,
                                0,
                                1137.1023715270699,
                                336.6179549129202,
                                0,
                                0,
                                1),
                VecBuilder.fill( // distort
                        0.14815813706474312,
                        -1.166871074914488,
                        0.002510801148857967,
                        0.0014575054249691716,
                        2.1737769139751677));
        LIFECAM_1280_720p.setCalibError(0.37, 0.25);
        LIFECAM_1280_720p.setFPS(30);
        LIFECAM_1280_720p.setAvgLatencyMs(60);
        LIFECAM_1280_720p.setLatencyStdDevMs(20);
    }
}
