# Aiming at a Target

The following example is from the PhotonLib example repository ([Java](https://github.com/PhotonVision/photonvision/tree/master/photonlib-java-examples/aimattarget)).

## Knowledge and Equipment Needed

- Robot with a vision system running PhotonVision
- AprilTag 7
- Ability to track a AprilTags with a well-tuned pipeline

## Code

Now that you have properly set up your vision system and have tuned a pipeline, you can now aim your robot/turret at an AprilTag using the data from PhotonVision. This data is reported over NetworkTables and includes: latency, whether there is a AprilTag detected or not, pitch, yaw, area, skew, and target pose relative to the robot. This data will be used/manipulated by the vendor dependency, PhotonLib. The documentation for the Network Tables API can be found {ref}`here <docs/additional-resources/nt-api:Getting Target Information>` and the documentation for PhotonLib {ref}`here <docs/programming/photonlib/adding-vendordep:What is PhotonLib?>`.

For this simple example, only yaw is needed.

In this example, while the operator holds a button down, the robot will turn towards the goal using the P term of a PID loop. To learn more about how PID loops work, how WPILib implements them, and more, visit  [Advanced Controls (PID)](https://docs.wpilib.org/en/stable/docs/software/advanced-control/introduction/index.html) and [PID Control in WPILib](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/controllers/pidcontroller.html#pid-control-in-wpilib).

```{eval-rst}
.. tab-set::

    .. tab-item:: Java

       .. rli:: https://raw.githubusercontent.com/gerth2/photonvision/adb3098fbe0cdbc1a378c6d5a41126dd1d6d6955/photonlib-java-examples/aimattarget/src/main/java/frc/robot/Robot.java
         :language: java
         :lines: 77-117
         :linenos:
         :lineno-start: 77

    .. tab-item:: C++ (Header)

       .. rli:: https://raw.githubusercontent.com/gerth2/photonvision/adb3098fbe0cdbc1a378c6d5a41126dd1d6d6955/photonlib-cpp-examples/aimattarget/src/main/include/Robot.h
         :language: c++
         :lines: 25-60
         :linenos:
         :lineno-start: 25

    .. tab-item:: C++ (Source)

       .. rli:: https://raw.githubusercontent.com/gerth2/photonvision/adb3098fbe0cdbc1a378c6d5a41126dd1d6d6955/photonlib-cpp-examples/aimattarget/src/main/cpp/Robot.cpp
         :language: c++
         :lines: 56-96
         :linenos:
         :lineno-start: 56

    .. tab-item:: Python

       .. rli:: https://raw.githubusercontent.com/gerth2/photonvision/adb3098fbe0cdbc1a378c6d5a41126dd1d6d6955/photonlib-python-examples/aimattarget/robot.py
         :language: python
         :lines: 46-70
         :linenos:
         :lineno-start: 46

```
