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

import edu.wpi.first.math.controller.RamseteController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrajectoryConfig;
import edu.wpi.first.math.trajectory.TrajectoryGenerator;
import edu.wpi.first.wpilibj.Timer;
import java.util.List;

/**
 * Implements logic to convert a set of desired waypoints (ie, a trajectory) and the current
 * estimate of where the robot is at (ie, the estimated Pose) into motion commands for a drivetrain.
 * The Ramaste controller is used to smoothly move the robot from where it thinks it is to where it
 * thinks it ought to be.
 */
public class AutoController {
    private Trajectory trajectory;

    private RamseteController ramsete = new RamseteController();

    private Timer timer = new Timer();

    boolean isRunning = false;

    Trajectory.State desiredDtState;

    public AutoController() {
        // Change this trajectory if you need the robot to follow different paths.
        trajectory =
                TrajectoryGenerator.generateTrajectory(
                        new Pose2d(2, 2, new Rotation2d()),
                        List.of(),
                        new Pose2d(6, 4, new Rotation2d()),
                        new TrajectoryConfig(2, 2));
    }

    /** @return The starting (initial) pose of the currently-active trajectory */
    public Pose2d getInitialPose() {
        return trajectory.getInitialPose();
    }

    /** Starts the controller running. Call this at the start of autonomous */
    public void startPath() {
        timer.reset();
        timer.start();
        isRunning = true;
    }

    /** Stops the controller from generating commands */
    public void stopPath() {
        isRunning = false;
        timer.reset();
    }

    /**
     * Given the current estimate of the robot's position, calculate drivetrain speed commands which
     * will best-execute the active trajectory. Be sure to call `startPath()` prior to calling this
     * method.
     *
     * @param curEstPose Current estimate of drivetrain pose on the field
     * @return The commanded drivetrain motion
     */
    public ChassisSpeeds getCurMotorCmds(Pose2d curEstPose) {
        if (isRunning) {
            double elapsed = timer.get();
            desiredDtState = trajectory.sample(elapsed);
        } else {
            desiredDtState = new Trajectory.State();
        }

        return ramsete.calculate(curEstPose, desiredDtState);
    }

    /**
     * @return The position which the auto controller is attempting to move the drivetrain to right
     *     now.
     */
    public Pose2d getCurPose2d() {
        return desiredDtState.poseMeters;
    }
}
