# Simulating Aiming and Getting in Range

The following example comes from the PhotonLib example repository ([Java](https://github.com/PhotonVision/photonvision/tree/661f8b2c0495474015f6ea9a89d65f9788436a05/photonlib-java-examples/src/main/java/org/photonlib/examples/simaimandrange)/[C++](https://github.com/PhotonVision/photonvision/tree/661f8b2c0495474015f6ea9a89d65f9788436a05/photonlib-cpp-examples/src/main/cpp/examples/simaimandrange)). Full code is available at those links.

## Knowledge and Equipment Needed

- Everything required in {ref}`Combining Aiming and Getting in Range <docs/examples/aimandrange:Knowledge and Equipment Needed>`.

## Background

The previous examples show how to run PhotonVision on a real robot, with a physical robot drivetrain moving around and interacting with the software.

This example builds upon that, adding support for simulating robot motion and incorporating that motion into a {code}`SimVisionSystem`. This allows you to test control algorithms on your development computer, without requiring access to a real robot.

```{raw} html
<video width="85%" controls>
    <source src="../../_static/assets/simaimandrange.mp4" type="video/mp4">
    Your browser does not support the video tag.
</video>
```

## Walkthrough

First, in the main {code}`Robot` source file, we add support to periodically update a new simulation-specific object. This logic only gets used while running in simulation:

```{eval-rst}
.. tab-set-code::

    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-java-examples/simaimandrange/src/main/java/frc/robot/Robot.java
      :language: java
      :lines: 118-128
      :linenos:
      :lineno-start: 118
```

Then, we add in the implementation of our new `DrivetrainSim` class. Please reference the [WPILib documentation on physics simulation](https://docs.wpilib.org/en/stable/docs/software/wpilib-tools/robot-simulation/physics-sim.html).

Simulated Vision support is added with the following steps:

### Creating the Simulated Vision System

First, we create a new {code}`SimVisionSystem` to represent our camera and coprocessor running PhotonVision.

```{eval-rst}
.. tab-set-code::

    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-java-examples/simaimandrange/src/main/java/frc/robot/sim/DrivetrainSim.java
      :language: java
      :lines: 73-93
      :linenos:
      :lineno-start: 72
```

Next, we create objects to represent the physical location and size of the vision targets we are calibrated to detect. This example models the down-field high goal vision target from the 2020 and 2021 games.

```{eval-rst}
.. tab-set-code::

    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-java-examples/simaimandrange/src/main/java/frc/robot/sim/DrivetrainSim.java
      :language: java
      :lines: 95-111
      :linenos:
      :lineno-start: 95
```

Finally, we add our target to the simulated vision system.

```{eval-rst}
.. tab-set-code::

    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-java-examples/simaimandrange/src/main/java/frc/robot/sim/DrivetrainSim.java
      :language: java
      :lines: 116-117
      :linenos:
      :lineno-start: 113

```

If you have additional targets you want to detect, you can add them in the same way as the first one.

### Updating the Simulated Vision System

Once we have all the properties of our simulated vision system defined, the work to do at runtime becomes very minimal. Simply pass in the robot's pose periodically to the simulated vision system.

```{eval-rst}
.. tab-set-code::

    .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-java-examples/simaimandrange/src/main/java/frc/robot/sim/DrivetrainSim.java
      :language: java
      :lines: 124-142
      :linenos:
      :lineno-start: 122

```

The rest is done behind the scenes.
