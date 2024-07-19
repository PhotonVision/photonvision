AprilTags and PhotonPoseEstimator
=================================

.. note:: For more information on how to methods to get AprilTag data, look :ref:`here <docs/programming/photonlib/getting-target-data:Getting AprilTag Data From A Target>`.

PhotonLib includes a ``PhotonPoseEstimator`` class, which allows you to combine the pose data from all tags in view in order to get a field relative pose. The ``PhotonPoseEstimator`` class works with one camera per object instance, but more than one instance may be created.

Creating an ``AprilTagFieldLayout``
-----------------------------------
``AprilTagFieldLayout`` is used to represent a layout of AprilTags within a space (field, shop at home, classroom, etc.). WPILib provides a JSON that describes the layout of AprilTags on the field which you can then use in the AprilTagFieldLayout constructor. You can also specify a custom layout.

The API documentation can be found in here: `Java <https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/apriltag/AprilTagFieldLayout.html>`_ and `C++ <https://github.wpilib.org/allwpilib/docs/release/cpp/classfrc_1_1_april_tag_field_layout.html>`_.

.. tab-set-code::
   .. code-block:: java

      // The field from AprilTagFields will be different depending on the game.
      AprilTagFieldLayout aprilTagFieldLayout = AprilTagFields.k2024Crescendo.loadAprilTagLayoutField();

   .. code-block:: c++

      // The parameter for LoadAPrilTagLayoutField will be different depending on the game.
      frc::AprilTagFieldLayout aprilTagFieldLayout = frc::LoadAprilTagLayoutField(frc::AprilTagField::k2024Crescendo);


Creating a ``PhotonPoseEstimator``
----------------------------------
The PhotonPoseEstimator has a constructor that takes an ``AprilTagFieldLayout`` (see above), ``PoseStrategy``, ``PhotonCamera``, and ``Transform3d``. ``PoseStrategy`` has six possible values:

* MULTI_TAG_PNP_ON_COPROCESSOR
    *  Calculates a new robot position estimate by combining all visible tag corners. Recommended for all teams as it will be the most accurate.
    *  Must configure the AprilTagFieldLayout properly in the UI, please see :ref:`here <docs/apriltag-pipelines/multitag:multitag localization>` for more information.
* LOWEST_AMBIGUITY
    * Choose the Pose with the lowest ambiguity.
* CLOSEST_TO_CAMERA_HEIGHT
    * Choose the Pose which is closest to the camera height.
* CLOSEST_TO_REFERENCE_POSE
    * Choose the Pose which is closest to the pose from setReferencePose().
* CLOSEST_TO_LAST_POSE
    * Choose the Pose which is closest to the last pose calculated.
* AVERAGE_BEST_TARGETS
    * Choose the Pose which is the average of all the poses from each tag.

.. tab-set-code::
   .. code-block:: java

      //Forward Camera
      cam = new PhotonCamera("testCamera");
      Transform3d robotToCam = new Transform3d(new Translation3d(0.5, 0.0, 0.5), new Rotation3d(0,0,0)); //Cam mounted facing forward, half a meter forward of center, half a meter up from center.

      // Construct PhotonPoseEstimator
      PhotonPoseEstimator photonPoseEstimator = new PhotonPoseEstimator(aprilTagFieldLayout, PoseStrategy.CLOSEST_TO_REFERENCE_POSE, cam, robotToCam);

   .. code-block:: c++

      // Forward Camera
      std::shared_ptr<photonlib::PhotonCamera> cameraOne =
          std::make_shared<photonlib::PhotonCamera>("testCamera");
      // Camera is mounted facing forward, half a meter forward of center, half a
      // meter up from center.
      frc::Transform3d robotToCam =
          frc::Transform3d(frc::Translation3d(0.5_m, 0_m, 0.5_m),
                          frc::Rotation3d(0_rad, 0_rad, 0_rad));

      // ... Add other cameras here

      // Assemble the list of cameras & mount locations
      std::vector<
          std::pair<std::shared_ptr<photonlib::PhotonCamera>, frc::Transform3d>>
          cameras;
      cameras.push_back(std::make_pair(cameraOne, robotToCam));

      photonlib::RobotPoseEstimator estimator(
          aprilTags, photonlib::CLOSEST_TO_REFERENCE_POSE, cameras);

Using a ``PhotonPoseEstimator``
-------------------------------
Calling ``update()`` on your ``PhotonPoseEstimator`` will return an ``EstimatedRobotPose``, which includes a ``Pose3d`` of the latest estimated pose (using the selected strategy) along with a ``double`` of the timestamp when the robot pose was estimated. You should be updating your `drivetrain pose estimator <https://docs.wpilib.org/en/latest/docs/software/advanced-controls/state-space/state-space-pose-estimators.html>`_ with the result from the ``PhotonPoseEstimator`` every loop using ``addVisionMeasurement()``.

.. tab-set-code::
   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/357d8a518a93f7a1f8084a79449249e613b605a7/photonlib-java-examples/apriltagExample/src/main/java/frc/robot/PhotonCameraWrapper.java
      :language: java
      :lines: 85-88

   .. code-block:: c++

      std::pair<frc::Pose2d, units::millisecond_t> getEstimatedGlobalPose(
          frc::Pose3d prevEstimatedRobotPose) {
        robotPoseEstimator.SetReferencePose(prevEstimatedRobotPose);
        units::millisecond_t currentTime = frc::Timer::GetFPGATimestamp();
        auto result = robotPoseEstimator.Update();
        if (result.second) {
          return std::make_pair<>(result.first.ToPose2d(),
                                  currentTime - result.second);
        } else {
          return std::make_pair(frc::Pose2d(), 0_ms);
        }
      }

You should be updating your `drivetrain pose estimator <https://docs.wpilib.org/en/latest/docs/software/advanced-controls/state-space/state-space-pose-estimators.html>`_ with the result from the ``RobotPoseEstimator`` every loop using ``addVisionMeasurement()``. TODO: add example note

Additional ``PhotonPoseEstimator`` Methods
------------------------------------------

``setReferencePose(Pose3d referencePose)``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Updates the stored reference pose when using the CLOSEST_TO_REFERENCE_POSE strategy.

``setLastPose(Pose3d lastPose)``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Update the stored last pose. Useful for setting the initial estimate when using the CLOSEST_TO_LAST_POSE strategy.
