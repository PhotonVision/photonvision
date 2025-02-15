# Aiming at a Target

The following example is from the PhotonLib example repository ([Java](https://github.com/PhotonVision/photonvision/tree/main/photonlib-java-examples/aimattarget)).

## Knowledge and Equipment Needed

- A Robot
- A camera mounted rigidly to the robot's frame, centered and pointed forward.
- A coprocessor running PhotonVision with an AprilTag or Aruco 2D Pipeline.
- [A printout of AprilTag 7](https://firstfrc.blob.core.windows.net/frc2025/FieldAssets/Apriltag_Images_and_User_Guide.pdf), mounted on a rigid and flat surface.

## Code

Now that you have properly set up your vision system and have tuned a pipeline, you can now aim your robot at an AprilTag using the data from PhotonVision. The _yaw_ of the target is the critical piece of data that will be needed first.

Yaw is reported to the roboRIO over Network Tables. PhotonLib, our vender dependency, is the easiest way to access this data. The documentation for the Network Tables API can be found {ref}`here <docs/additional-resources/nt-api:Getting Target Information>` and the documentation for PhotonLib {ref}`here <docs/programming/photonlib/adding-vendordep:What is PhotonLib?>`.

In this example, while the operator holds a button down, the robot will turn towards the AprilTag using the P term of a PID loop. To learn more about how PID loops work, how WPILib implements them, and more, visit [Advanced Controls (PID)](https://docs.wpilib.org/en/stable/docs/software/advanced-control/introduction/index.html) and [PID Control in WPILib](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/controllers/pidcontroller.html#pid-control-in-wpilib).

```{eval-rst}
.. tab-set::

    .. tab-item:: Java

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-java-examples/aimattarget/src/main/java/frc/robot/Robot.java
         :language: java
         :lines: 77-117
         :linenos:
         :lineno-start: 77

    .. tab-item:: C++ (Header)

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-cpp-examples/aimattarget/src/main/include/Robot.h
         :language: c++
         :lines: 25-60
         :linenos:
         :lineno-start: 25

    .. tab-item:: C++ (Source)

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-cpp-examples/aimattarget/src/main/cpp/Robot.cpp
         :language: c++
         :lines: 56-96
         :linenos:
         :lineno-start: 56

    .. tab-item:: Python

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-python-examples/aimattarget/robot.py
         :language: python
         :lines: 46-70
         :linenos:
         :lineno-start: 46

```
