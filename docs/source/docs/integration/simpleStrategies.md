# Simple Strategies

Simple strategies for using vision processor outputs involve using the target's position in the 2D image to infer *range* and *angle* to a particular AprilTag.

## Knowledge and Equipment Needed

- A Coprocessor running PhotonVision
- A Drivetrain with wheels
- An AprilTag to aim at

## Angle Alignment

The simplest way to align a robot to an AprilTag is to rotate the drivetrain until the tag is centered in the camera image. To do this,

1. Read the current yaw angle to the AprilTag from the vision Coprocessor.
2. If too far off to one side, command the drivetrain to rotate in the opposite direction to compensate.

See the  {ref}`Aiming at a Target <docs/examples/aimingatatarget:Knowledge and Equipment Needed>` example for more information.

NOTE: This works if the camera is centered on the robot. This is easiest from a software perspective. If the camera is not centered, take a peek at the next example - it shows how to account for an offset.

## Adding Range Alignment

By looking at the position of the AprilTag in the "vertical" direction in the image, and applying some trigonometry, the distance between the robot and the camera can be deduced.

1. Read the current pitch angle to the AprilTag from the vision coprocessor.
2. Do math to calculate the distance to the AprilTag.
2. If too far in one direction, command the drivetrain to travel in the opposite direction to compensate.

This can be done simultaneously while aligning to the desired angle.

See the {ref}`Aim and Range <docs/examples/aimandrange:Knowledge and Equipment Needed>` example for more information.
