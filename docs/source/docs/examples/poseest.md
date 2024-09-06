# Using WPILib Pose Estimation, Simulation, and PhotonVision Together

The following example comes from the PhotonLib example repository ([Java](https://github.com/gerth2/photonvision/tree/master/photonlib-java-examples/)).  Full code is available at that links.

## Knowledge and Equipment Needed

- Everything required in {ref}`Combining Aiming and Getting in Range <docs/examples/aimandrange:Knowledge and Equipment Needed>`, plus some familiarity with WPILib pose estimation functionality.

## Background

This example demonstrates integration of swerve drive control, a basic swerve physics simulation, and PhotonLib's simulated vision system functionality.

## Walkthrough

### Estimating Pose

The {code}`Drivetrain` class includes functionality to fuse multiple sensor readings together (including PhotonVision) into a best-guess of the pose on the field..

Please reference the [WPILib documentation](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/state-space/state-space-pose_state-estimators.html) on using the {code}`SwerveDrivePoseEstimator` class.

We use the 2024 game's AprilTag Locations:

```{eval-rst}
.. tab-set::

    .. tab-item:: Java
       :sync: java

       .. rli:: https://raw.githubusercontent.com/gerth2/photonvision/d6c37ead118ae58d224c0c4fd7f7a2b7931940b5/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
         :language: java
         :lines: 68-68
         :linenos:
         :lineno-start: 68

```

To incorporate PhotonVision, we need to create a {code}`PhotonCamera`:


```{eval-rst}
.. tab-set::

    .. tab-item:: Java
       :sync: java

       .. rli:: https://raw.githubusercontent.com/gerth2/photonvision/d6c37ead118ae58d224c0c4fd7f7a2b7931940b5/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
         :language: java
         :lines: 57-57
         :linenos:
         :lineno-start: 57

```

During periodic execution, we read back camera results. If we see AprilTags in the image, we calculate the camera-measured pose of the robot and pass it to the {code}`Drivetrain`.

```{eval-rst}
.. tab-set::

    .. tab-item:: Java
       :sync: java

       .. rli:: https://raw.githubusercontent.com/gerth2/photonvision/d6c37ead118ae58d224c0c4fd7f7a2b7931940b5/photonlib-java-examples/poseest/src/main/java/frc/robot/Robot.java
         :language: java
         :lines: 64-74
         :linenos:
         :lineno-start: 64

```

### Simulating the Camera

First, we create a new {code}`VisionSystemSim` to represent our camera and coprocessor running PhotonVision, and moving around our simulated field.

```{eval-rst}
.. tab-set::

    .. tab-item:: Java
       :sync: java

       .. rli:: https://raw.githubusercontent.com/gerth2/photonvision/d6c37ead118ae58d224c0c4fd7f7a2b7931940b5/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
         :language: java
         :lines: 65-69
         :linenos:
         :lineno-start: 65

```

Then, we add configure the simulated vision system to match the camera system being simulated.

```{eval-rst}
.. tab-set::

    .. tab-item:: Java
       :sync: java

       .. rli:: https://raw.githubusercontent.com/gerth2/photonvision/d6c37ead118ae58d224c0c4fd7f7a2b7931940b5/photonlib-java-examples/poseest/src/main/java/frc/robot/Vision.java
         :language: java
         :lines: 69-82
         :linenos:
         :lineno-start: 69

```


### Updating the Simulated Vision System

During simulation, we periodically update the simulated vision system.

```{eval-rst}
.. tab-set::

    .. tab-item:: Java
       :sync: java

       .. rli:: https://raw.githubusercontent.com/gerth2/photonvision/80e16ece87c735e30755dea271a56a2ce217b588/photonlib-java-examples/poseest/src/main/java/frc/robot/Robot.java
         :language: java
         :lines: 114-132
         :linenos:
         :lineno-start: 114

```

The rest is done behind the scenes.
