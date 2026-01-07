# AprilTags and PhotonPoseEstimator

:::{note}
For more information on how to methods to get AprilTag data, look {ref}`here <docs/programming/photonlib/getting-target-data:Getting AprilTag Data From A Target>`.
:::

PhotonLib includes a `PhotonPoseEstimator` class, which allows you to combine the pose data from all tags in view in order to get a field relative pose. For each camera, a separate instance of the `PhotonPoseEstimator` class should be created.

## Creating an `AprilTagFieldLayout`

`AprilTagFieldLayout` is used to represent a layout of AprilTags within a space (field, shop at home, classroom, etc.). WPILib provides a JSON that describes the layout of AprilTags on the field which you can then use in the AprilTagFieldLayout constructor. You can also specify a custom layout.

The API documentation can be found in here: [Java](https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/apriltag/AprilTagFieldLayout.html), [C++](https://github.wpilib.org/allwpilib/docs/release/cpp/classfrc_1_1_april_tag_field_layout.html), and [Python](https://robotpy.readthedocs.io/projects/apriltag/en/stable/robotpy_apriltag/AprilTagFieldLayout.html#robotpy_apriltag.AprilTagFieldLayout).

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

Another necessary argument for creating a `PhotonPoseEstimator` is the `Transform3d` representing the robot-relative location and orientation of the camera. A `Transform3d` contains a `Translation3d` and a `Rotation3d`. The `Translation3d` is created in meters and the `Rotation3d` is created with radians. For more information on the coordinate system, please see the {ref}`Coordinate Systems <docs/apriltag-pipelines/coordinate-systems:Coordinate Systems>` documentation.

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

The PhotonPoseEstimator has a constructor that takes an `AprilTagFieldLayout` (see above) and `Transform3d`.

```{eval-rst}
.. tab-set-code::
   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
    :language: java
    :lines: 63

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-cpp-examples/poseest/src/main/include/Vision.h
    :language: c++
    :lines: 149-150

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-python-examples/poseest/robot.py
    :language: python
    :lines: 45-48
```

## Using a `PhotonPoseEstimator`

To use your `PhotonPoseEstimator`, you must create a `PhotonCamera` and feed the results into your `PhotonPoseEstimator`. To do this, you must first set the name of your camera in Photon Client. From there you can define the camera in code.

```{eval-rst}
.. tab-set-code::
    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
     :language: java
     :lines: 62

    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-cpp-examples/poseest/src/main/include/Vision.h
     :language: c++
     :lines: 151

    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-python-examples/poseest/robot.py
     :language: python
     :lines: 44
```

When taking in a result from a `PhotonCamera`, PhotonPoseEstimator offers nine possible "strategies" for calculating a pose from a pipeline result in the form of methods that you can call, following the pattern `estimate<strategy name>Pose`:

- Coprocessor MultiTag (`estimateCoprocMultiTagPose`)
  - Calculates a new robot position estimate by combining all visible tag corners. Recommended for all teams as it will be the most accurate.
  - Must configure the AprilTagFieldLayout properly in the UI, please see {ref}`here <docs/apriltag-pipelines/multitag:multitag localization>` for more information.
- Lowest Ambiguity (`estimateLowestAmbiguityPose`)
  - Choose the Pose with the lowest ambiguity.
- Closest to Camera Height (`estimateClosestToCameraHeightPose`)
  - Choose the Pose which is closest to the camera height.
- Closest to Reference Pose (`estimateClosestToReferencePose`)
  - Choose the Pose which is closest to the pose that is passed into the function.
- Average Best Targets (`estimateAverageBestTargetsPose`)
  - Choose the Pose which is the average of all the poses from each tag.
- roboRio MultiTag (`estimateRioMultiTagPose`)
  - A slower, older version of Coprocessor MultiTag, not recommended for use.
- PnP Distance Trig Solve (`estimatePnpDistanceTrigSolvePose`)
  - Use distance data from best visible tag to compute a Pose. This runs on the RoboRIO in order
    to access the robot's yaw heading, and MUST have addHeadingData called every frame so heading
    data is up-to-date. Based on a reference implementation by [FRC Team 6328 Mechanical Advantage](https://www.chiefdelphi.com/t/frc-6328-mechanical-advantage-2025-build-thread/477314/98).
- Constrained SolvePnP (`estimateConstrainedSolvepnpPose`)
  - Solve a constrained version of the Perspective-n-Point problem with the robot's drivebase
    flat on the floor. This computation takes place on the RoboRIO, and should not take more than 2ms.
    This also requires addHeadingData to be called every frame so heading data is up to date.

Calling one of the `estimate<strategy>Pose()` methods on your `PhotonPoseEstimator` will return an `Optional<EstimatedRobotPose>`, which includes a `Pose3d` of the latest estimated pose (using the selected strategy) along with a `double` of the timestamp when the robot pose was estimated. The recommended way to use the estimatePose methods is to
1. do estimation with one of MultiTag methods, check if the result is empty, then
2. fallback to single tag estimation using a method like `estimateLowestAmbiguityPose`.

```{eval-rst}
.. tab-set-code::
   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
    :language: java
    :lines: 91-94

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-cpp-examples/poseest/src/main/include/Vision.h
    :language: c++
    :lines: 79-82

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-python-examples/poseest/robot.py
     :language: python
     :lines: 52-54
```

For Constrained SolvePnP, it's recommended to do the previously mentioned steps, and then feed the pose (if it exists) into `estimateConstrainedSolvepnpPose`, and if the Constrained SolvePnP result is empty, simply feed the seed pose into your drivetrain pose estimator.

Once you have the `Optional<EstimatedRobotPose>`, you can check to see if there's an actual pose inside, and act accordingly. You should be updating your [drivetrain pose estimator](https://docs.wpilib.org/en/latest/docs/software/advanced-controls/state-space/state-space-pose-estimators.html) with the result from the `PhotonPoseEstimator` every loop using `addVisionMeasurement()`. For Java and C++, the examples pass a method from the drivetrain to a `Vision` object, with the parameter being called `estConsumer`. Python calls the drivetrain directly.

```{eval-rst}
.. tab-set-code::
   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-java-examples/poseest/src/main/java/frc/robot/Robot.java
    :language: java
    :lines: 49

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-cpp-examples/poseest/src/main/include/Robot.h
    :language: c++
    :lines: 54-57

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-python-examples/poseest/robot.py
     :language: python
     :lines: 56-58
```

```{eval-rst}
.. tab-set-code::
   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
    :language: java
    :lines: 89-115

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-cpp-examples/poseest/src/main/include/Vision.h
    :language: c++
    :lines: 77-100

   .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/refs/heads/main/photonlib-python-examples/poseest/robot.py
     :language: python
     :lines: 51-54
```

## Complete Examples

The complete examples for the `PhotonPoseEstimator` can be found in the following locations:

- [Java](https://github.com/PhotonVision/photonvision/tree/main/photonlib-java-examples/poseest)
- [C++](https://github.com/PhotonVision/photonvision/tree/main/photonlib-cpp-examples/poseest)
- [Python](https://github.com/PhotonVision/photonvision/tree/main/photonlib-python-examples/poseest)

## Additional `PhotonPoseEstimator` Methods

For more information on the `PhotonPoseEstimator` class, please see the API documentation.

- [Java Documentation](https://javadocs.photonvision.org/release/org/photonvision/PhotonPoseEstimator.html)
- [C++ Documentation](https://cppdocs.photonvision.org/release/classphoton_1_1_photon_pose_estimator.html)
- [Python Documentation](https://pydocs.photonvision.org/release/reference/photonPoseEstimator/)
