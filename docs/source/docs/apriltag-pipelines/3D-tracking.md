# 3D Tracking

3D AprilTag tracking will allow you to track the real-world position and rotation of a tag relative to the camera's image sensor. This is useful for robot pose estimation and other applications like autonomous scoring. In order to use 3D tracking, you must first {ref}`calibrate your camera <docs/calibration/calibration:Calibrating Your Camera>`. Once you have, you need to enable 3D mode in the UI and you will now be able to get 3D pose information from the tag! For information on getting and using this information in your code, see {ref}`the programming reference. <docs/programming/index:Programming Reference>`.

## Ambiguity

Translating from 2D to 3D using data from the calibration and the four tag corners can lead to "pose ambiguity", where it appears that the AprilTag pose is flipping between two different poses. You can read more about this issue `here. <https://docs.wpilib.org/en/stable/docs/software/vision-processing/apriltag/apriltag-intro.html#d-to-3d-ambiguity>` Ambiguity is calculated as the ratio of reprojection errors between two pose solutions (if they exist), where reprojection error is the error corresponding to the image distance between where the apriltag's corners are detected vs where we expect to see them based on the tag's estimated camera relative pose.

There are a few steps you can take to resolve/mitigate this issue:

1. Mount cameras at oblique angles so it is less likely that the tag will be seen straight on.
2. Use the {ref}`MultiTag system <docs/apriltag-pipelines/multitag:MultiTag Localization>` in order to combine the corners from multiple tags to get a more accurate and unambiguous pose.
3. Reject all tag poses where the ambiguity ratio (available via PhotonLib) is greater than 0.2.
