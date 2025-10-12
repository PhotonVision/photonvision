# Using WPILib Pose Estimation, Simulation, and PhotonVision Together

The following example comes from the PhotonLib example repository ([Java](https://github.com/PhotonVision/photonvision/tree/main/photonlib-java-examples/poseest)/[C++](https://github.com/PhotonVision/photonvision/tree/main/photonlib-cpp-examples/poseest)/[Python](https://github.com/PhotonVision/photonvision/tree/main/photonlib-python-examples/poseest)).  Full code is available at that links.

## Knowledge and Equipment Needed

- Everything required in {ref}`Combining Aiming and Getting in Range <docs/examples/aimandrange:Knowledge and Equipment Needed>`, plus some familiarity with WPILib pose estimation functionality.

## Background

This example demonstrates integration of swerve drive control, a basic swerve physics simulation, and PhotonLib's simulated vision system functionality.

## Walkthrough

### Estimating Pose

The {code}`Drivetrain` class includes functionality to fuse multiple sensor readings together (including PhotonVision) into a best-guess of the pose on the field.

Please reference the [WPILib documentation](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/state-space/state-space-pose_state-estimators.html) on using the {code}`SwerveDrivePoseEstimator` class.

We use the 2024 game's AprilTag Locations:

```{eval-rst}
.. tab-set::

    .. tab-item:: Java
       :sync: java

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
         :language: java
         :lines: 68-68
         :linenos:
         :lineno-start: 68

    .. tab-item:: C++

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-cpp-examples/poseest/src/main/include/Constants.h
         :language: c++
         :lines: 42-43
         :linenos:
         :lineno-start: 42

    .. tab-item:: Python

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-python-examples/poseest/robot.py
         :language: python
         :lines: 46-46
         :linenos:
         :lineno-start: 46

```



To incorporate PhotonVision, we need to create a {code}`PhotonCamera`:


```{eval-rst}
.. tab-set::

    .. tab-item:: Java
       :sync: java

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
         :language: java
         :lines: 57-57
         :linenos:
         :lineno-start: 57

    .. tab-item:: C++

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-cpp-examples/poseest/src/main/include/Vision.h
         :language: c++
         :lines: 145-145
         :linenos:
         :lineno-start: 145

    .. tab-item:: Python

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-python-examples/poseest/robot.py
         :language: python
         :lines: 44-44
         :linenos:
         :lineno-start: 44
```

During periodic execution, we read back camera results. If we see AprilTags in the image, we calculate the camera-measured pose of the robot and pass it to the {code}`Drivetrain`.

```{eval-rst}
.. tab-set::

    .. tab-item:: Java
       :sync: java

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-java-examples/poseest/src/main/java/frc/robot/Robot.java
         :language: java
         :lines: 64-74
         :linenos:
         :lineno-start: 64

    .. tab-item:: C++

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-cpp-examples/poseest/src/main/cpp/Robot.cpp
         :language: c++
         :lines: 38-46
         :linenos:
         :lineno-start: 38

    .. tab-item:: Python

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-python-examples/poseest/robot.py
         :language: python
         :lines: 54-56
         :linenos:
         :lineno-start: 54

```

### Simulating the Camera

First, we create a new {code}`VisionSystemSim` to represent our camera and coprocessor running PhotonVision, and moving around our simulated field.

```{eval-rst}
.. tab-set::

    .. tab-item:: Java
       :sync: java

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
         :language: java
         :lines: 65-69
         :linenos:
         :lineno-start: 65

    .. tab-item:: C++

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-cpp-examples/poseest/src/main/include/Vision.h
         :language: c++
         :lines: 49-52
         :linenos:
         :lineno-start: 49

    .. tab-item:: Python

       # Coming Soon!

```

Then, we add configure the simulated vision system to match the camera system being simulated.

```{eval-rst}
.. tab-set::

    .. tab-item:: Java
       :sync: java

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
         :language: java
         :lines: 69-82
         :linenos:
         :lineno-start: 69

    .. tab-item:: C++

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-cpp-examples/poseest/src/main/include/Vision.h
         :language: c++
         :lines: 53-65
         :linenos:
         :lineno-start: 53

    .. tab-item:: Python

       # Coming Soon!
```


### Updating the Simulated Vision System

During simulation, we periodically update the simulated vision system.

```{eval-rst}
.. tab-set::

    .. tab-item:: Java
       :sync: java

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-java-examples/poseest/src/main/java/frc/robot/Robot.java
         :language: java
         :lines: 114-132
         :linenos:
         :lineno-start: 114

    .. tab-item:: C++

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-cpp-examples/poseest/src/main/cpp/Robot.cpp
         :language: c++
         :lines: 95-109
         :linenos:
         :lineno-start: 95

    .. tab-item:: Python

       # Coming Soon!
```

The rest is done behind the scenes.

```{raw} html
<video width="85%" controls>
    <source src="../../_static/assets/poseest_demo.mp4" type="video/mp4">
    Your browser does not support the video tag.
</video>
```
