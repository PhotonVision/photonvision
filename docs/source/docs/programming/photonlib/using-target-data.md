# Using Target Data

A `PhotonUtils` class with helpful common calculations is included within `PhotonLib` to aid teams in using AprilTag data in order to get positional information on the field. This class contains two methods, `calculateDistanceToTargetMeters()`/`CalculateDistanceToTarget()` and `estimateTargetTranslation2d()`/`EstimateTargetTranslation()` (Java and C++ respectively).

## Estimating Field Relative Pose with AprilTags

`estimateFieldToRobotAprilTag(Transform3d cameraToTarget, Pose3d fieldRelativeTagPose, Transform3d cameraToRobot)` returns your robot's `Pose3d` on the field using the pose of the AprilTag relative to the camera, pose of the AprilTag relative to the field, and the transform from the camera to the origin of the robot.

```{eval-rst}
.. tab-set-code::
   .. code-block:: Java

      // Calculate robot's field relative pose
      if (aprilTagFieldLayout.getTagPose(target.getFiducialId()).isPresent()) {
        Pose3d robotPose = PhotonUtils.estimateFieldToRobotAprilTag(target.getBestCameraToTarget(), aprilTagFieldLayout.getTagPose(target.getFiducialId()).get(), cameraToRobot);
      }
   .. code-block:: C++

     //TODO

   .. code-block:: Python

      # Coming Soon!
```

## Estimating Field Relative Pose (Traditional)

You can get your robot's `Pose2D` on the field using various camera data, target yaw, gyro angle, target pose, and camera position. This method estimates the target's relative position using `estimateCameraToTargetTranslation` (which uses pitch and yaw to estimate range and heading), and the robot's gyro to estimate the rotation of the target.

```{eval-rst}
.. tab-set-code::
   .. code-block:: Java

      // Calculate robot's field relative pose
      Pose2D robotPose = PhotonUtils.estimateFieldToRobot(
        kCameraHeight, kTargetHeight, kCameraPitch, kTargetPitch, Rotation2d.fromDegrees(-target.getYaw()), gyro.getRotation2d(), targetPose, cameraToRobot);

   .. code-block:: C++

      // Calculate robot's field relative pose
      frc::Pose2D robotPose = photonlib::EstimateFieldToRobot(
        kCameraHeight, kTargetHeight, kCameraPitch, kTargetPitch, frc::Rotation2d(units::degree_t(-target.GetYaw())), frc::Rotation2d(units::degree_t(gyro.GetRotation2d)), targetPose, cameraToRobot);

   .. code-block:: Python

      # Coming Soon!

```

## Calculating Distance to Target

If your camera is at a fixed height on your robot and the height of the target is fixed, you can calculate the distance to the target based on your camera's pitch and the pitch to the target.

```{eval-rst}
.. tab-set-code::

   .. code-block:: Java

      // TODO

   .. code-block:: C++

      // TODO

   .. code-block:: Python

      # Coming Soon!

```

:::{note}
The C++ version of PhotonLib uses the Units library. For more information, see [here](https://docs.wpilib.org/en/stable/docs/software/basic-programming/cpp-units.html).
:::

## Calculating Distance Between Two Poses

`getDistanceToPose(Pose2d robotPose, Pose2d targetPose)` allows you to calculate the distance between two poses. This is useful when using AprilTags, given that there may not be an AprilTag directly on the target.

```{eval-rst}
.. tab-set-code::
   .. code-block:: Java

      double distanceToTarget = PhotonUtils.getDistanceToPose(robotPose, targetPose);

   .. code-block:: C++

      //TODO

   .. code-block:: Python

      # Coming Soon!
```

## Estimating Camera Translation to Target

You can get a [translation](https://docs.wpilib.org/en/latest/docs/software/advanced-controls/geometry/pose.html#translation) to the target based on the distance to the target (calculated above) and angle to the target (yaw).

```{eval-rst}
.. tab-set-code::
   .. code-block:: Java

      // Calculate a translation from the camera to the target.
      Translation2d translation = PhotonUtils.estimateCameraToTargetTranslation(
        distanceMeters, Rotation2d.fromDegrees(-target.getYaw()));

   .. code-block:: C++

      // Calculate a translation from the camera to the target.
      frc::Translation2d translation = photonlib::PhotonUtils::EstimateCameraToTargetTranslation(
        distance, frc::Rotation2d(units::degree_t(-target.GetYaw())));

   .. code-block:: Python

      # Coming Soon!

```

:::{note}
We are negating the yaw from the camera from CV (computer vision) conventions to standard mathematical conventions. In standard mathematical conventions, as you turn counter-clockwise, angles become more positive.
:::

## Getting the Yaw To a Pose

`getYawToPose(Pose2d robotPose, Pose2d targetPose)` returns the `Rotation2d` between your robot and a target. This is useful when turning towards an arbitrary target on the field (ex. the center of the hub in 2022).

```{eval-rst}
.. tab-set-code::
   .. code-block:: Java

      Rotation2d targetYaw = PhotonUtils.getYawToPose(robotPose, targetPose);
   .. code-block:: C++

     //TODO

   .. code-block:: Python

      # Coming Soon!
```
