# Output

The output card contains sections for target manipulation and offset modes.

## Target Manipulation

In this section, the Target Offset Point changes where the "center" of the target is. This can be useful if the pitch/yaw of the middle of the top edge of the target is desired, rather than the center of mass of the target. The "top"/"bottom"/"left"/"right" of the target are defined by the Target Orientation selection. For example, a 400x200px target in landscape mode would have the "top" offset point located at the middle of the uppermost long edge of the target, while in portrait mode the "top" offset point would be located in the middle of the topmost short edge (in this case, either the left or right sides).

This section also includes a switch to enable processing and sending multiple targets, up to 5, simultaneously. This information is available through PhotonLib. Note that the {code}`GetPitch`/{code}`GetYaw` methods will report the pitch/yaw of the "best" (lowest indexed) target.

```{raw} html
<video width="85%" controls>
    <source src="../../_static/assets/offsetandmultiple.mp4" type="video/mp4">
    Your browser does not support the video tag.
</video>
```

## Robot Offset

PhotonVision offers both single and dual point offset modes. In single point mode, the "Take Point" button will set the crosshair location to the center of the current "best" target.

In dual point mode, two snapshots are required. Take one snapshot with the target far away, and the other with the target closer. The position of the crosshair will be linearly interpolated between these two points based on the area of the current "best" target. This might be useful if single point is not accurate across the range of the tracking distance, or for significantly offset cameras.
