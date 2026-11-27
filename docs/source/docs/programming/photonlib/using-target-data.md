# Using Target Data

A `PhotonUtils` class with helpful common calculations is included within Java and C++ PhotonLib to aid teams in using AprilTag data in order to get positional information on the field. This class contains `calculateDistanceToTargetMeters()`/`CalculateDistanceToTarget()` and `estimateTargetTranslation2d()`/`EstimateTargetTranslation()` (Java and C++ respectively). photonlibpy does not ship `PhotonUtils`; the Python tabs below use the matching wpimath calls.

## Estimating Field Relative Pose with AprilTags

`estimateFieldToRobotAprilTag(Transform3d cameraToTarget, Pose3d fieldRelativeTagPose, Transform3d cameraToRobot)` returns your robot's `Pose3d` on the field using the pose of the AprilTag relative to the camera, pose of the AprilTag relative to the field, and the transform from the camera to the origin of the robot.

```{eval-rst}
.. tab-set-code::
   .. code-block:: java

      // Calculate robot's field relative pose
      if (aprilTagFieldLayout.getTagPose(target.getFiducialId()).isPresent()) {
        Pose3d robotPose = PhotonUtils.estimateFieldToRobotAprilTag(target.getBestCameraToTarget(), aprilTagFieldLayout.getTagPose(target.getFiducialId()).get(), cameraToRobot);
      }
   .. code-block:: c++

     //TODO

   .. code-block:: python

      # Calculate robot's field relative pose
      tagPose = aprilTagFieldLayout.getTagPose(target.getFiducialId())
      if tagPose is not None:
          robotPose = tagPose + target.getBestCameraToTarget().inverse() + cameraToRobot
```

## Estimating Field Relative Pose (Traditional)

You can get your robot's `Pose2D` on the field using various camera data, target yaw, gyro angle, target pose, and camera position. This method estimates the target's relative position using `estimateCameraToTargetTranslation` (which uses pitch and yaw to estimate range and heading), and the robot's gyro to estimate the rotation of the target.

```{eval-rst}
.. tab-set-code::
   .. code-block:: java

      // Calculate robot's field relative pose
      Pose2D robotPose = PhotonUtils.estimateFieldToRobot(
        kCameraHeight, kTargetHeight, kCameraPitch, kTargetPitch, Rotation2d.fromDegrees(-target.getYaw()), gyro.getRotation2d(), targetPose, cameraToRobot);

   .. code-block:: c++

      // Calculate robot's field relative pose
      wpi::math::Pose2d robotPose = photonlib::EstimateFieldToRobot(
        kCameraHeight, kTargetHeight, kCameraPitch, kTargetPitch, wpi::math::Rotation2d(wpi::units::degree_t(-target.GetYaw())), wpi::math::Rotation2d(wpi::units::degree_t(gyro.GetRotation2d)), targetPose, cameraToRobot);

   .. code-block:: python

      # PhotonUtils.estimateFieldToRobot is Java/C++ only.
      # Use the pitch/yaw helpers below, then compose with your gyro and cameraToRobot transform.

```

## Calculating Distance to Target

If your camera is at a fixed height on your robot and the height of the target is fixed, you can calculate the distance to the target based on your camera's pitch and the pitch to the target.

```{eval-rst}
.. tab-set-code::

   .. code-block:: java

      // TODO

   .. code-block:: c++

      // TODO

   .. code-block:: python

      import math

      distanceMeters = (kTargetHeight - kCameraHeight) / math.tan(
          kCameraPitch + math.radians(target.getPitch())
      )

```

:::{note}
The C++ version of PhotonLib uses the Units library. For more information, see [here](https://docs.wpilib.org/en/stable/docs/software/basic-programming/cpp-units.html).
:::

## Calculating Distance Between Two Poses

`getDistanceToPose(Pose2d robotPose, Pose2d targetPose)` allows you to calculate the distance between two poses. This is useful when using AprilTags, given that there may not be an AprilTag directly on the target.

```{eval-rst}
.. tab-set-code::
   .. code-block:: java

      double distanceToTarget = PhotonUtils.getDistanceToPose(robotPose, targetPose);

   .. code-block:: c++

      //TODO

   .. code-block:: python

      distanceToTarget = robotPose.translation().distance(targetPose.translation())
```

## Estimating Camera Translation to Target

You can get a [translation](https://docs.wpilib.org/en/latest/docs/software/advanced-controls/geometry/pose.html#translation) to the target based on the distance to the target (calculated above) and angle to the target (yaw).

```{eval-rst}
.. tab-set-code::
   .. code-block:: java

      // Calculate a translation from the camera to the target.
      Translation2d translation = PhotonUtils.estimateCameraToTargetTranslation(
        distanceMeters, Rotation2d.fromDegrees(-target.getYaw()));

   .. code-block:: c++

      // Calculate a translation from the camera to the target.
      wpi::math::Translation2d translation = photonlib::PhotonUtils::EstimateCameraToTargetTranslation(
        distance, wpi::math::Rotation2d(wpi::units::degree_t(-target.GetYaw())));

   .. code-block:: python

      from wpimath.geometry import Rotation2d, Translation2d

      # Calculate a translation from the camera to the target.
      translation = Translation2d(
          distanceMeters, Rotation2d.fromDegrees(-target.getYaw())
      )

```

:::{note}
We are negating the yaw from the camera from CV (computer vision) conventions to standard mathematical conventions. In standard mathematical conventions, as you turn counter-clockwise, angles become more positive.
:::

## Getting the Yaw To a Pose

`getYawToPose(Pose2d robotPose, Pose2d targetPose)` returns the `Rotation2d` between your robot and a target. This is useful when turning towards an arbitrary target on the field (ex. the center of the hub in 2022).

```{eval-rst}
.. tab-set-code::
   .. code-block:: java

      Rotation2d targetYaw = PhotonUtils.getYawToPose(robotPose, targetPose);
   .. code-block:: c++

     //TODO

   .. code-block:: python

      from wpimath.geometry import Rotation2d

      relative = targetPose.relativeTo(robotPose).translation()
      targetYaw = Rotation2d(relative.X(), relative.Y())
```
