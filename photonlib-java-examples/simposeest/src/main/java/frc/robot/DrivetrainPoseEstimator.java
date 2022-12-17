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

package frc.robot;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.estimator.DifferentialDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.numbers.N5;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.AnalogGyro;
import org.photonvision.PhotonCamera;

/**
 * Performs estimation of the drivetrain's current position on the field, using a vision system,
 * drivetrain encoders, and a gyroscope. These sensor readings are fused together using a Kalman
 * filter. This in turn creates a best-guess at a Pose2d of where our drivetrain is currently at.
 */
public class DrivetrainPoseEstimator {
    // Sensors used as part of the Pose Estimation
    private final AnalogGyro gyro = new AnalogGyro(Constants.kGyroPin);
    private PhotonCamera cam = new PhotonCamera(Constants.kCamName);
    // Note - drivetrain encoders are also used. The Drivetrain class must pass us
    // the relevant readings.

    // Kalman Filter Configuration. These can be "tuned-to-taste" based on how much
    // you trust your
    // various sensors. Smaller numbers will cause the filter to "trust" the
    // estimate from that particular
    // component more than the others. This in turn means the particualr component
    // will have a stronger
    // influence on the final pose estimate.
    Matrix<N5, N1> stateStdDevs = VecBuilder.fill(0.05, 0.05, Units.degreesToRadians(5), 0.05, 0.05);
    Matrix<N3, N1> localMeasurementStdDevs = VecBuilder.fill(0.01, 0.01, Units.degreesToRadians(0.1));
    Matrix<N3, N1> visionMeasurementStdDevs =
            VecBuilder.fill(0.01, 0.01, Units.degreesToRadians(0.1));

    private final DifferentialDrivePoseEstimator m_poseEstimator;

    public DrivetrainPoseEstimator(double leftDist, double rightDist) {
        m_poseEstimator =
                new DifferentialDrivePoseEstimator(
                        Constants.kDtKinematics,
                        gyro.getRotation2d(),
                        0, // Assume zero encoder counts at start
                        0,
                        new Pose2d()); // Default - start at origin. This will get reset by the autonomous init
    }

    /**
     * Perform all periodic pose estimation tasks.
     *
     * @param actWheelSpeeds Current Speeds (in m/s) of the drivetrain wheels
     * @param leftDist Distance (in m) the left wheel has traveled
     * @param rightDist Distance (in m) the right wheel has traveled
     */
    public void update(double leftDist, double rightDist) {
        m_poseEstimator.update(gyro.getRotation2d(), leftDist, rightDist);

        var res = cam.getLatestResult();
        if (res.hasTargets()) {
            var imageCaptureTime = res.getTimestampSeconds();
            var camToTargetTrans = res.getBestTarget().getBestCameraToTarget();
            var camPose = Constants.kFarTargetPose.transformBy(camToTargetTrans.inverse());
            m_poseEstimator.addVisionMeasurement(
                    camPose.transformBy(Constants.kCameraToRobot).toPose2d(), imageCaptureTime);
        }
    }

    /**
     * Force the pose estimator to a particular pose. This is useful for indicating to the software
     * when you have manually moved your robot in a particular position on the field (EX: when you
     * place it on the field at the start of the match).
     */
    public void resetToPose(Pose2d pose, double leftDist, double rightDist) {
        m_poseEstimator.resetPosition(gyro.getRotation2d(), leftDist, rightDist, pose);
    }

    /** @return The current best-guess at drivetrain position on the field. */
    public Pose2d getPoseEst() {
        return m_poseEstimator.getEstimatedPosition();
    }
}
