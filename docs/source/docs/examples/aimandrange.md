# Combining Aiming and Getting in Range

The following example is from the PhotonLib example repository ([Java](https://github.com/PhotonVision/photonvision/tree/master/photonlib-java-examples/aimandrange)/[C++](https://github.com/PhotonVision/photonvision/tree/master/photonlib-cpp-examples/aimandrange)).

## Knowledge and Equipment Needed

- Everything required in {ref}`Aiming at a Target <docs/examples/aimingatatarget:Knowledge and Equipment Needed>`.

## Code

Now that you know how to aim toward the AprilTag, let's also drive the correct distance from the AprilTag.

To do this, we'll use the *pitch* of the target in the camera image and trigonometry to figure out how far away the robot is from the AprilTag. Then, like before, we'll use the P term of a PID controller to drive the robot to the correct distance.

```{eval-rst}
.. tab-set::

    .. tab-item:: Java

       .. rli:: https://raw.githubusercontent.com/gerth2/photonvision/d6c37ead118ae58d224c0c4fd7f7a2b7931940b5/photonlib-java-examples/aimandrange/src/main/java/frc/robot/Robot.java
         :language: java
         :lines: 84-131
         :linenos:
         :lineno-start: 84

    .. tab-item:: C++ (Header)

       TODO

    .. tab-item:: C++ (Source)

       TODO

    .. tab-item:: Python

       TODO

```