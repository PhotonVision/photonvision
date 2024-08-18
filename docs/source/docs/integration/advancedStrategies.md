# Advanced Strategies

Advanced strategies for using vision processing results involve working with the robot's *pose* on the field. A *pose* is a combination an X/Y coordinate, and an angle describing where the robot's front is pointed. It is always considered *relative* to some fixed point on the field.

WPILib provides a [Pose2d](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/geometry/pose.html)  class to describe poses in software.

## Knowledge and Equipment Needed

- A Coprocessor running PhotonVision
  \- Accurate camera calibration to support "3D mode" required
- A Drivetrain with wheels and sensors
  \- Sufficient sensors to measure wheel rotation
  \- Capable of closed-loop velocity control
- A gyroscope or IMU measuring actual robot heading
- Experience using some path-planning library (WPILib is our recommendation)

## Path Planning in a Target-Centered Reference Frame

When using 3D mode in PhotonVision, the [SolvePNP Algorithm](https://en.wikipedia.org/wiki/Perspective-n-Point)  is used to deduce the *camera's* position in a 3D coordinate system centered on the target itself.

A simple algorithm for using this measurement is:

1. Assume your robot needs to be at a fixed `Pose2D` *relative to the target*.
2. When triggered:
   #. Read the most recent vision measurement - this is your *actual* pose.
   #. Generate a simple trajectory to the goal position
   #. Execute the trajectory

:::{note}
There is not currently an example demonstrating this technique.
:::

## Global Pose Estimation

A more complex way to utilize a camera-supplied `Pose2D` is to incorporate it into an estimation of the robot's `Pose2D` in a global field reference frame.

When using this strategy, the measurements made by the camera are *fused* with measurements from other sensors, a model of expected robot behavior, and a matrix of weights that describes how trustworthy each sensor is. The result is a *best-guess* at the current pose on the field.

In turn, this best-guess position is used to path plan to the known positions on the field, which may or may not have vision targets nearby.

See the  {ref}`Pose Estimation <docs/examples/simposeest:Knowledge and Equipment Needed>` example for more information.
