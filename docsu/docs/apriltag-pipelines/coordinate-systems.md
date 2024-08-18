# Coordinate Systems

## Field and Robot Coordinate Frame

PhotonVision follows the WPILib conventions for the robot and field coordinate systems, as defined [here](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/geometry/coordinate-systems.html).

You define the camera to robot transform in the robot coordinate frame.

## Camera Coordinate Frame

OpenCV by default uses x-left/y-down/z-out for camera transforms. PhotonVision applies a base rotation to this transformation to make robot to tag transforms more in line with the WPILib coordinate system. The x, y, and z axes are also shown in red, green, and blue in the 3D mini-map and targeting overlay in the UI.

- The origin is the focal point of the camera lens
- The x-axis points out of the camera
- The y-axis points to the left
- The z-axis points upwards

```{image} images/camera-coord.png
:align: center
:scale: 45 %
```

```{image} images/multiple-tags.png
:align: center
:scale: 45 %
```

## AprilTag Coordinate Frame

The AprilTag coordinate system is defined as follows, relative to the center of the AprilTag itself, and when viewing the tag as a robot would. Again, PhotonVision changes this coordinate system to be more in line with WPILib. This means that a robot facing a tag head-on would see a robot-to-tag transform with a translation only in x, and a rotation of 180 degrees about z. The tag coordinate system is also shown with x/y/z in red/green/blue in the UI target overlay and mini-map.

- The origin is the center of the tag
- The x-axis is normal to the plane the tag is printed on, pointing outward from the visible side of the tag.
- The y-axis points to the right
- The z-axis points upwards

```{image} images/apriltag-coords.png
:align: center
:scale: 45 %
```
