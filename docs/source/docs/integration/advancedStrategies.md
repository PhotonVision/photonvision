# Advanced Strategies

Advanced strategies for using vision processing results involve working with the robot's *pose* on the field.

A *pose* is a combination an X/Y coordinate, and an angle describing where the robot's front is pointed. A pose is always considered *relative* to some fixed point on the field.

WPILib provides a [Pose2d](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/geometry/pose.html) class to describe poses in software.

PhotonVision can supply correcting information to keep estimates of *pose* accurate over a full match.

## Knowledge and Equipment Needed

- A Coprocessor running PhotonVision
  \- Accurate camera calibration to support "3D mode" required
- A Drivetrain with wheels and sensors
  \- Sufficient sensors to measure wheel rotation
  \- Capable of closed-loop velocity control
- A gyroscope or IMU measuring actual robot heading
- Experience using some path-planning library

## Robot Poses from the Camera

When using 3D mode in PhotonVision, an additional step is run to estimate the 3D position of camera, relative to one or more AprilTags.

This process does not produce a *unique* solution. There are multiple possible camera positions which might explain the image it observed. Additionally, the camera is rarely mounted in the exact center of a robot.

For these reasons, the 3D information must be filtered and transformed before they can describe the robot's pose.

PhotonLib provides {ref}`a utility class to assist with this process on the roboRIO <docs/programming/photonlib/robot-pose-estimator:AprilTags and PhotonPoseEstimator>`. Alternatively, {ref}`a "multi-tag" strategy can do this process on the coprocessor. <docs/apriltag-pipelines/multitag:Enabling MultiTag>`.

## Field-Relative Pose Estimation

The camera's guess of the robot pose generally should be *fused* with other sensor readings.

WPILib provides [a set of pose estimation classes](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/state-space/state-space-pose-estimators.html) for doing this work.

## I have a Pose Estimate, Now What?

### Triggering Actions Automatically

A simple way to use a pose estimate is to activate robot functions automatically when in the correct spot on the field.

```{eval-rst}
.. tab-set-code::

   .. code-block:: Java

      Pose3d robotPose;
      boolean launcherSpinCmd;

      // ...

      if(robotPose.X() < 1.5){
        // Near blue alliance wall, start spinning the launcher wheel
        launcherSpinCmd = True;
      } else {
        // Far away, no need to run launcher.
        launcherSpinCmd = False;
      }

      // ...
```

### PathPlanning

A common, but more complex usage of a pose estimate is an input to a path-following algorithm. Specifically, the pose estimate is used to correct for the robot straying off of the pre-defined path.

See the {ref}`Pose Estimation <docs/examples/poseest:Knowledge and Equipment Needed>` example for details on integrating this.
