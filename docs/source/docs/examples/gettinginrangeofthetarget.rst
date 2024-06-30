Getting in Range of the Target
==============================

The following example is from the PhotonLib example repository (`Java <https://github.com/PhotonVision/photonvision/tree/master/photonlib-java-examples/getinrange>`_/`C++ <https://github.com/PhotonVision/photonvision/tree/master/photonlib-cpp-examples/getinrange>`_).


Knowledge and Equipment Needed
-----------------------------------------------

- Everything required in :ref:`Aiming at a Target <docs/examples/aimingatatarget:Knowledge and Equipment Needed>`.
- Large space where your robot can move around freely

Code
-------

In FRC, a mechanism usually has to be a certain distance away from its target in order to be effective and score. In the previous example, we showed how to aim your robot at the target. Now we will show how to move to a certain distance from the target.

For proper functionality of just this example, ensure that your robot is pointed towards the target.

While the operator holds down a button, the robot will drive towards the target and get in range.

This example uses P term of the PID loop and PhotonLib and the distance function of PhotonUtils.

.. warning:: The PhotonLib utility to calculate distance depends on the camera being at a different vertical height than the target. If this is not the case, a different method for estimating distance, such as target width or area, should be used. In general, this method becomes more accurate as range decreases and as the height difference increases.

.. note:: There is no strict minimum delta-height necessary for this method to be applicable, just a requirement that a delta exists.

.. tab-set::

    .. tab-item:: Java

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-java-examples/getinrange/src/main/java/frc/robot/Robot.java
         :language: java
         :lines: 42-107
         :linenos:
         :lineno-start: 42

    .. tab-item:: C++ (Header)

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-cpp-examples/getinrange/src/main/include/Robot.h
         :language: c++
         :lines: 27-67
         :linenos:
         :lineno-start: 27

    .. tab-item:: C++ (Source)

       .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/ebef19af3d926cf87292177c9a16d01b71219306/photonlib-cpp-examples/getinrange/src/main/cpp/Robot.cpp
         :language: c++
         :lines: 25-58
         :linenos:
         :lineno-start: 25

.. hint:: The accuracy of the measurement of the camera's pitch (:code:`CAMERA_PITCH_RADIANS` in the above example), as well as the camera's FOV, will determine the overall accuracy of this method.
