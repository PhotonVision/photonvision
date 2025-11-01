# Combining Aiming and Getting in Range

The following example is from the PhotonLib example repository ([Java](https://github.com/PhotonVision/photonvision/tree/main/photonlib-java-examples/aimandrange)/[C++](https://github.com/PhotonVision/photonvision/tree/main/photonlib-cpp-examples/aimandrange)/[Python](https://github.com/PhotonVision/photonvision/tree/main/photonlib-python-examples/aimandrange))

## Knowledge and Equipment Needed

- Everything required in {ref}`Aiming at a Target <docs/examples/aimingatatarget:Knowledge and Equipment Needed>`.

## Code

Now that you know how to aim toward the AprilTag, let's also drive the correct distance from the AprilTag.

To do this, we'll use the _pitch_ of the target in the camera image and trigonometry to figure out how far away the robot is from the AprilTag. Then, like before, we'll use the P term of a PID controller to drive the robot to the correct distance.

```{eval-rst}
.. tab-set::
    :sync-group: code

    .. tab-item:: Java
       :sync: java

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-java-examples/aimandrange/src/main/java/frc/robot/Robot.java
         :language: java
         :lines: 84-131
         :linenos:
         :lineno-start: 84

    .. tab-item:: C++ (Header)
       :sync: c++

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-cpp-examples/aimandrange/src/main/include/Robot.h
         :language: c++
         :lines: 25-63
         :linenos:
         :lineno-start: 25

    .. tab-item:: C++ (Source)
       :sync: c++

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-cpp-examples/aimandrange/src/main/cpp/Robot.cpp
         :language: c++
         :lines: 58-107
         :linenos:
         :lineno-start: 58

    .. tab-item:: Python
       :sync: python

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/abe95dfaa055bbe3609f72cfcaaba0f96ee7978c/photonlib-python-examples/aimandrange/robot.py
         :language: python
         :lines: 52-91
         :linenos:
         :lineno-start: 52

```
