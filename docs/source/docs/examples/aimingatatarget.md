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

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/06f0f7d66f68f43e7e461bce672b07e9d2954cda/photonlib-java-examples/aimattarget/src/main/java/frc/robot/Robot.java
         :language: java
         :lines: 41-98
         :linenos:
         :lineno-start: 41

    .. tab-item:: C++ (Header)

       TODO

    .. tab-item:: C++ (Source)

       TODO

    .. tab-item:: Python

       TODO

```
