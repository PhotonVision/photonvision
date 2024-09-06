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

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/06f0f7d66f68f43e7e461bce672b07e9d2954cda/photonlib-java-examples/aimandrange/src/main/java/frc/robot/Robot.java
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
