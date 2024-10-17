# Calibration and Image Rotation

To stay consistent with the OpenCV camera coordinate frame, we put the origin in the top left, with X right, Y down, and Z out (as required by the right-hand rule). Intuitively though, if I ask you to rotate an image 90 degrees clockwise though, you'd probably rotate it about -Z in this coordinate system. Just be aware of this inconsistency.

![](images/image_corner_frames.png)

If we have any one point in any of those coordinate systems, we can transform it into any of the other ones using standard geometry libraries by performing relative transformations (like in this pseudocode):

```
Translation2d tag_corner1 = new Translation2d();
Translation2d rotated = tag_corner1.relativeTo(ORIGIN_ROTATED_90_CCW);
```
