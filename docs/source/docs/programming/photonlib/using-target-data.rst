Using Target Data
=================

A ``PhotonUtils`` class with helpful common calculations is included within ``PhotonLib`` to aid teams in using target data in order to get positional information on the field. This class contains two methods, ``calculateDistanceToTargetMeters()``/``CalculateDistanceToTarget()`` and ``estimateTargetTranslation2d()``/``EstimateTargetTranslation()`` (Java and C++ respectively).

Estimating Field Relative Pose with AprilTags
---------------------------------------------
``estimateFieldToRobotAprilTag(Transform3d cameraToTarget, Pose3d fieldRelativeTagPose, Transform3d cameraToRobot)`` returns your robot's ``Pose3d`` on the field using the pose of the AprilTag relative to the camera, pose of the AprilTag relative to the field, and the transform from the camera to the origin of the robot.

.. tab-set-code::
   .. code-block:: java

      // Calculate robot's field relative pose
      Pose3d robotPose = PhotonUtils.estimateFieldToRobotAprilTag(target.getBestCameraToTarget(), aprilTagFieldLayout.getTagPose(target.getFiducialId()), cameraToRobot);
   .. code-block:: c++

     //TODO

Estimating Field Relative Pose (Traditional)
--------------------------------------------

You can get your robot's ``Pose2D`` on the field using various camera data, target yaw, gyro angle, target pose, and camera position. This method estimates the target's relative position using ``estimateCameraToTargetTranslation`` (which uses pitch and yaw to estimate range and heading), and the robot's gyro to estimate the rotation of the target.

.. tab-set-code::
   .. code-block:: java

      // Calculate robot's field relative pose
      Pose2D robotPose = PhotonUtils.estimateFieldToRobot(
        kCameraHeight, kTargetHeight, kCameraPitch, kTargetPitch, Rotation2d.fromDegrees(-target.getYaw()), gyro.getRotation2d(), targetPose, cameraToRobot);

   .. code-block:: c++

      // Calculate robot's field relative pose
      frc::Pose2D robotPose = photonlib::EstimateFieldToRobot(
        kCameraHeight, kTargetHeight, kCameraPitch, kTargetPitch, frc::Rotation2d(units::degree_t(-target.GetYaw())), frc::Rotation2d(units::degree_t(gyro.GetRotation2d)), targetPose, cameraToRobot);


Calculating Distance to Target
------------------------------
If your camera is at a fixed height on your robot and the height of the target is fixed, you can calculate the distance to the target based on your camera's pitch and the pitch to the target.

.. tab-set-code::


     .. rli:: https://github.com/PhotonVision/photonvision/raw/a3bcd3ac4f88acd4665371abc3073bdbe5effea8/photonlib-java-examples/src/main/java/org/photonlib/examples/getinrange/Robot.java
        :language: java
        :lines: 78-94

     .. rli:: https://github.com/PhotonVision/photonvision/raw/a3bcd3ac4f88acd4665371abc3073bdbe5effea8/photonlib-cpp-examples/src/main/cpp/examples/getinrange/cpp/Robot.cpp
        :language: cpp
        :lines: 33-46

.. note:: The C++ version of PhotonLib uses the Units library. For more information, see `here <https://docs.wpilib.org/en/stable/docs/software/basic-programming/cpp-units.html>`_.

Calculating Distance Between Two Poses
--------------------------------------
``getDistanceToPose(Pose2d robotPose, Pose2d targetPose)`` allows you to calculate the distance between two poses. This is useful when using AprilTags, given that there may not be an AprilTag directly on the target.

.. tab-set-code::
   .. code-block:: java

      double distanceToTarget = PhotonUtils.getDistanceToPose(robotPose, targetPose);

   .. code-block:: c++

      //TODO

Estimating Camera Translation to Target
---------------------------------------
You can get a `translation <https://docs.wpilib.org/en/latest/docs/software/advanced-controls/geometry/pose.html#translation>`_ to the target based on the distance to the target (calculated above) and angle to the target (yaw).

.. tab-set-code::
   .. code-block:: java

      // Calculate a translation from the camera to the target.
      Translation2d translation = PhotonUtils.estimateCameraToTargetTranslation(
        distanceMeters, Rotation2d.fromDegrees(-target.getYaw()));

   .. code-block:: c++

      // Calculate a translation from the camera to the target.
      frc::Translation2d translation = photonlib::PhotonUtils::EstimateCameraToTargetTranslationn(
        distance, frc::Rotation2d(units::degree_t(-target.GetYaw())));

.. note:: We are negating the yaw from the camera from CV (computer vision) conventions to standard mathematical conventions. In standard mathematical conventions, as you turn counter-clockwise, angles become more positive.

Getting the Yaw To a Pose
-------------------------
``getYawToPose(Pose2d robotPose, Pose2d targetPose)`` returns the ``Rotation2d`` between your robot and a target. This is useful when turning towards an arbitrary target on the field (ex. the center of the hub in 2022).

.. tab-set-code::
   .. code-block:: java

      Rotation2d targetYaw = PhotonUtils.getYawToPose(robotPose, targetPose);
   .. code-block:: c++

     //TODO
