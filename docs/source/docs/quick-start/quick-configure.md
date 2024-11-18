# Quick Configure

## Settings to configure

### Team number

In order for photonvision to connect to the roborio it needs to know your team number.

### Camera Nickname

You **must** nickname your cameras in PhotonVision to ensure that every camera has a unique name. This is how you will identify cameras in robot code. The camera can be nicknamed using the edit button next to the camera name in the upper right of the Dashboard tab.

```{image} images/editCameraName.png
:align: center
```

## Pipeline Settings

### AprilTag

When using an Orange Pi 5 with an Arducam OV9281 teams will usually change the following settings. For more info on AprilTag settings please review {ref}`this<docs/apriltag-pipelines/2D-tracking-tuning:2D AprilTag Tuning / Tracking>`.

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
  - Don't use/rely on vision measurements while moving.

```{image} images/motionblur.png
:align: center
```

### Object Detection

When using an Orange Pi 5 with an OV9782 teams will usually change the following settings. For more info on object detection settings please review {ref}`this<docs/objectDetection/about-object-detection:About Object Detection>`.

- Resolution:
  - Resolutions higher than 640x640 may not result in any higher detection accuracy and may lower {ref}`performance<docs/objectDetection/about-object-detection:Letterboxing>`.
- Confidence:
  - 0.75 - 0.95 Lower values are for detecting worn game pieces or less ideal game pieces. Higher for less worn, more ideal game pieces.
- White Balance Temperature:
  - Adjust this to achieve better color accuracy. This may be needed to increase confidence.
- Set arducam specific camera type selector to OV9782
