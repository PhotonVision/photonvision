# AprilTags and PhotonPoseEstimator

:::{note}
For more information on how to methods to get AprilTag data, look {ref}`here <docs/programming/photonlib/getting-target-data:Getting AprilTag Data From A Target>`.
:::

PhotonLib includes a `PhotonPoseEstimator` class, which allows you to combine the pose data from all tags in view in order to get a field relative pose.

## Creating an `AprilTagFieldLayout`

`AprilTagFieldLayout` is used to represent a layout of AprilTags within a space (field, shop at home, classroom, etc.). WPILib provides a JSON that describes the layout of AprilTags on the field which you can then use in the AprilTagFieldLayout constructor. You can also specify a custom layout.

The API documentation can be found in here: [Java](https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/apriltag/AprilTagFieldLayout.html) and [C++](https://github.wpilib.org/allwpilib/docs/release/cpp/classfrc_1_1_april_tag_field_layout.html).

```{eval-rst}
.. tab-set-code::
   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-java-examples/poseest/src/main/java/frc/robot/Constants.java
    :language: java
    :lines: 48-49

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-cpp-examples/poseest/src/main/include/Constants.h
    :language: c++
    :lines: 46-47

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-python-examples/poseest/robot.py
    :language: python
    :lines: 46

```

## Defining the Robot to Camera `Transform3d`

Another necessary argument for creating a `PhotonPoseEstimator` is the `Transform3d` representing the robot-relative location and orientation of the camera. A `Transform3d` contains a `Translation3d` and a `Rotation3d`. The `Translation3d` is created in meters and the `Rotation3d` is created with degrees.

```{eval-rst}
.. tab-set-code::
    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-java-examples/poseest/src/main/java/frc/robot/Constants.java
     :language: java
     :lines: 44-45

    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-cpp-examples/poseest/src/main/include/Constants.h
     :language: c++
     :lines: 43-45

    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-python-examples/poseest/robot.py
     :language: python
     :lines: 33-36
```

## Creating a `PhotonPoseEstimator`

The PhotonPoseEstimator has a constructor that takes an `AprilTagFieldLayout` (see above), `PoseStrategy`, and `Transform3d`. `PoseStrategy` has six possible values:

- MULTI_TAG_PNP_ON_COPROCESSOR
    - Calculates a new robot position estimate by combining all visible tag corners. Recommended for all teams as it will be the most accurate.
    - Must configure the AprilTagFieldLayout properly in the UI, please see {ref}`here <docs/apriltag-pipelines/multitag:multitag localization>` for more information.
- LOWEST_AMBIGUITY
    - Choose the Pose with the lowest ambiguity.
- CLOSEST_TO_CAMERA_HEIGHT
    - Choose the Pose which is closest to the camera height.
- CLOSEST_TO_REFERENCE_POSE
    - Choose the Pose which is closest to the pose from setReferencePose().
- CLOSEST_TO_LAST_POSE
    - Choose the Pose which is closest to the last pose calculated.
- AVERAGE_BEST_TARGETS
    - Choose the Pose which is the average of all the poses from each tag.

```{eval-rst}
.. tab-set-code::
   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
    :language: java
    :lines: 59-60

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-cpp-examples/poseest/src/main/include/Vision.h
    :language: c++
    :lines: 141-144

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-python-examples/poseest/robot.py
    :language: python
    :lines: 45-50
```

:::{note}
Python still takes a `PhotonCamera` in the constructor, so you must create the camera as shown in the next section and then return and use it to create the `PhotonPoseEstimator`.
:::

## Using a `PhotonPoseEstimator`

The final prerequisite to using your `PhotonPoseEstimator` is creating a `PhotonCamera`. To do this, you must set the name of your camera in Photon Client. From there you can define the camera in code.

```{eval-rst}
.. tab-set-code::
    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
     :language: java
     :lines: 57

    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-cpp-examples/aimattarget/src/main/include/Robot.h
     :language: c++
     :lines: 55

    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-python-examples/poseest/robot.py
     :language: python
     :lines: 44
```

Calling `update()` on your `PhotonPoseEstimator` will return an `EstimatedRobotPose`, which includes a `Pose3d` of the latest estimated pose (using the selected strategy) along with a `double` of the timestamp when the robot pose was estimated. You should be updating your [drivetrain pose estimator](https://docs.wpilib.org/en/latest/docs/software/advanced-controls/state-space/state-space-pose-estimators.html) with the result from the `PhotonPoseEstimator` every loop using `addVisionMeasurement()`.

```{eval-rst}
.. tab-set-code::
   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
    :language: java
    :lines: 96-114

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-cpp-examples/poseest/src/main/include/Vision.h
    :language: c++
    :lines: 71-93

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-python-examples/poseest/robot.py
     :language: python
     :lines: 53

```

You should be updating your [drivetrain pose estimator](https://docs.wpilib.org/en/latest/docs/software/advanced-controls/state-space/state-space-pose-estimators.html) with the result from the `PhotonPoseEstimator` every loop using `addVisionMeasurement()`. TODO: add example note

## Additional `PhotonPoseEstimator` Methods

### `setReferencePose(Pose3d referencePose)`

Updates the stored reference pose when using the CLOSEST_TO_REFERENCE_POSE strategy.

### `setLastPose(Pose3d lastPose)`

Update the stored last pose. Useful for setting the initial estimate when using the CLOSEST_TO_LAST_POSE strategy.

### `setMultiTagFallbackStrategy(PoseStrategy strategy)`

Determines the fallback strategy for pose estimation. You are strongly encouraged to set this.
