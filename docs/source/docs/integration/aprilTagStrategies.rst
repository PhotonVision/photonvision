AprilTag Strategies
====================

.. note:: The same strategies covered in the simple and advanced strategy sections still apply to AprilTags, and we encourage you to read them first. This page will discuss the specific nuances to using AprilTags.

Simple Strategies
-----------------

Prior to the introduction of AprilTags, the most common vision strategy for teams was to use the yaw of the detected target in order to turn to the target, and then score. This is still possible with AprilTags as the yaw of the tag is reported. Similarly, getting the distance to the target via trigonometry will also work. This is discussed in greater detail in the previous page.

Advanced Strategies
-------------------
AprilTags allows you find the robot pose on the field using data from the tags. A pose is a combination an X/Y coordinate, and an angle describing where the robot’s front is pointed. It is always considered relative to some fixed point on the field.

Knowledge and Equipment Needed
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Knowledge

* How to tune an AprilTag Pipeline (found in the pipeline tuning section)

Equipment

* A Coprocessor running PhotonVision - Accurate camera calibration to support “3D mode” required

* A Drivetrain with wheels and sensors (Sufficient sensors to measure wheel rotation and capable of closed-loop velocity control)

* A gyroscope or IMU measuring actual robot heading

Global Pose Estimation / Pose Estimation Strategies
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. note:: See the previous page for more general information. Most of the information is the same except now the camera is supplying a ``Pose3D``.

The nature of how AprilTags will be laid out makes it very likely that you will get multiple pose measurements within a single frame from seeing multiple targets. This requires strategies to fuse these observations together and get a "best guess" as to where your robot is. The best way to do this is to use the corners from all visible AprilTags to estimate the robot's pose. This is done by using the ``PhotonPoseEstimator`` class and the "MULTI_TAG_PNP_ON_COPROCESSOR" strategy. Additional strategies include:

* A camera seeing multiple targets, taking the average of all the returned poses
* A camera seeing one target, with an assumed height off the ground, picking the pose which places it to the assumed height
* A camera seeing one target, and picking a pose most similar to the most recently observed pose
* A camera seeing one target, and picking a pose most similar to one provided externally (ie, from previous loop's odometry)
* A camera seeing one target, and picking the pose with the lowest ambiguity.

PhotonVision supports all of these different strategies via our ``PhotonPoseEstimator`` class that allows you to select one of the strategies above and get the relevant pose estimation.

Tuning Pose Estimators
^^^^^^^^^^^^^^^^^^^^^^

Coming soon!
TODO: Add this back in once simposeest example is added.
