Simple Strategies
=================

Simple strategies for using vision processor outputs involve using the target's position in the 2D image to infer *range* and *angle* to the target.

Knowledge and Equipment Needed
------------------------------

- A Coprocessor running PhotonVision
- A Drivetrain with wheels

Angle Alignment
---------------

The simplest way to use a vision processing result is to first determine how far left or right in the image the vision target should be for your robot to be "aligned" to the target. Then,

1. Read the current angle to the target from the vision Coprocessor.
2. If too far in one direction, command the drivetrain to rotate in the opposite direction to compensate.

See the  :ref:`Aiming at a Target <docs/examples/aimingatatarget:Knowledge and Equipment Needed>` example for more information.

.. note:: Sometimes, these strategies have also involved incorporating a gyroscope. This can be necessary due to the high latency of vision processing algorithms. However, advancements in the tools available (including PhotonVision) has made that unnecessary for most applications.

Range Alignment
---------------

By looking at the position of the target in the "vertical" direction in the image, and applying some trigonometry, the distance between the robot and the camera can be deduced.

1. Read the current distance to the target from the vision coprocessor.
2. If too far in one direction, command the drivetrain to travel in the opposite direction to compensate.

See the :ref:`Getting in Range of the Target <docs/examples/gettinginrangeofthetarget:Knowledge and Equipment Needed>` example for more information.


Angle + Range
-------------

Since the previous two alignment strategies work on independent axes of the robot, there's no reason you can't do them simultaneously.

See the :ref:`Aim and Range <docs/examples/aimandrange:Knowledge and Equipment Needed>` example for more information.
