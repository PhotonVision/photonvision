# Quick Configure

## Settings to configure

### Team number

In order for photonvision to connect to the roborio it needs to know your team number.

### Camera Names

You **must** rename your cameras to ensure that every camera has a unique name. This is how we will identify cameras in robot code.

## Pipeline Settings

### Apriltag

When using an Orange pi 5 with an arducam OV9281 teams will usually change the following settings.

- Resolution:
  - 1280x800
- Decimate:
  - 2
- Mode:
  - 3D
- Exposure and Gain:
  - Adjust these to achieve good brightness without flicker and low motion blur. This may vary based on lighting conditions in your competition environment.
- Enable MultiTag
- Set arducam specific camera type selector to OV9281

#### AprilTags and Motion Blur and Rolling Shutter

When detecting AprilTags, it's important to minimize 'motion blur' as much as possible. Motion blur appears as visual streaking or smearing in the camera feed, resulting from the movement of either the camera or the object in focus. Reducing this effect is essential, as the robot is often in motion, and a clearer image allows for detecting as many tags as possible. This is not to be confused with {ref}`rolling shutter<docs/hardware/selecting-hardware:Cameras Attributes>`.

- Fixes
  - Lower your exposure as low as possible. Using gain and brightness to account for lack of brightness.
- Other Options:
  - Don't use/rely vision measurements while moving.

```{image} images/motionblur.png
:align: center
```

### Object Detection

When using an Orange pi 5 with an OV9782 teams will usually change the following settings.

- Resolution:
  - Resolutions larger than 640x640 may not result in any more accurate detection and may lower {ref}`performance<docs/objectDetection/about-object-detection:Letterboxing>`.
- Confidence:
  - 0.75 - 0.95 Depending on if you want detection of warn game pieces or low false positives.
- White Balance Temperature:
  - Adjust this to achieve better color accuracy. This may be needed to increase confidence.
- Set arducam specific camera type selector to OV9782
