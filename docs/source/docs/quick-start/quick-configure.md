# Quick Configure

## Settings to configure

### Team number

In order for photonvision to connect to the roborio it needs to know your team number.

### Network Hostname

Rename each device from the default "Photonvision" to a unique hostname (e.g., "Photon-OrangePi-Left" or "Photon-RPi5-Back"). This helps differentiate multiple coprocessors on your network, making it easier to manage them.

## Pipeline Settings

### Apriltag

When using an Orange pi 5 with an OV9281 teams will usually change the following settings.

- Resolution:
  - 1280x800
- Decimate:
  - 2
- Mode:
  - 3D
- Exposure and Gain:
  - Adjust these to achieve good brightness without flicker and low motion blur. This may vary based on lighting conditions in your competition environment.
- Enable MultiTag

#### AprilTags and Motion Blur

When detecting AprilTags, you want to reduce the "motion blur" as much as possible. Motion blur is the visual streaking/smearing on the camera stream as a result of movement of the camera or object of focus. You want to mitigate this as much as possible because your robot is constantly moving and you want to be able to read as many tags as you possibly can. The possible solutions to this include:

1. Cranking your exposure as low as it goes and increasing your gain/brightness. This will decrease the effects of motion blur and increase FPS.
2. Using a global shutter (as opposed to rolling shutter) camera. This should eliminate most, if not all motion blur.
3. Only rely on tags when not moving.

```{image} images/motionblur.gif
:align: center
```

### Object Detection

- Resolution:
  - Resolutions larger than 640x640 may not result in any more accurate detection and may lower performance.
- Confidence:
  - 0.75 - 0.95 Depending on if you want detection of warn game pieces or low false positives.
- White Balance Temperature:
  - Adjust this to achieve better color accuracy. This may be needed to increase confidence.
