# Camera Tuning / Input

PhotonVision's "Input" tab contains settings that affect the image captured by the currently selected camera. This includes camera exposure and brightness, as well as resolution, orientation, and static cropping.

## Resolution

Resolution changes the resolution of the image captured. While higher resolutions are often more accurate than lower resolutions, they also run at a slower update rate.

When using the reflective/colored shape pipeline, detection should be run as low of a resolution as possible as you are only trying to detect simple contours (essentially colored blobs).

When using the AprilTag pipeline, you should try to use as high of a resolution as you can while still maintaining a reasonable FPS measurement. This is because higher resolution allows you to detect tags with higher accuracy and from larger distances.

## Exposure and brightness

Camera exposure and brightness control how bright the captured image will be, although they function differently. Camera exposure changes how long the camera shutter lets in light, which changes the overall brightness of the captured image. This is in contrast to brightness, which is a post-processing effect that boosts the overall brightness of the image at the cost of desaturating colors (making colors look less distinct).

:::{important}
For all pipelines, exposure time should be set as low as possible while still allowing for the target to be reliably tracked. This allows for faster processing as decreasing exposure will increase your camera FPS.
:::

For reflective pipelines, after adjusting exposure and brightness, the target should be lit green (or the color of the vision tracking LEDs used). The more distinct the color of the target, the more likely it will be tracked reliably.

:::{note}
Unlike with retroreflective tape, AprilTag tracking is not very dependent on lighting consistency. If you have trouble detecting tags due to low light, you may want to try increasing exposure, but this will likely decrease your achievable framerate.
:::

### AprilTags and Motion Blur

For AprilTag pipelines, your goal is to reduce the "motion blur" as much as possible. Motion blur is the visual streaking/smearing on the camera stream as a result of movement of the camera or object of focus. You want to mitigate this as much as possible because your robot is constantly moving and you want to be able to read as many tags as you possibly can. The possible solutions to this include:

1. Cranking your exposure as low as it goes and increasing your gain/brightness. This will decrease the effects of motion blur and increase FPS.
2. Using a global shutter (as opposed to rolling shutter) camera. This should eliminate most, if not all motion blur.
3. Only rely on tags when not moving.

```{image} images/motionblur.gif
:align: center
```

## Orientation

Orientation can be used to rotate the image prior to vision processing. This can be useful for cases where the camera is not oriented parallel to the ground. Do note that this operation can in some cases significantly reduce FPS.

## Static Crop

Static crop restricts vision processing to a fixed rectangular region of the camera frame. When enabled, the input image is cropped to the region defined by the **Crop X Range** and **Crop Y Range** sliders (in pixels) before any target detection runs. The two range sliders set the left/right and top/bottom bounds of the region, and their maximums track the current frame dimensions. Both ranges default to the full frame (0 to the frame width or height), so enabling static crop without moving the sliders leaves the image untouched.

Very small crops are grown to a minimum of 16 pixels per axis before processing, since a sliver of an image is useless for vision (and some detectors misbehave when given one). On an AprilTag pipeline the crop's top-left corner is also nudged up and left onto a multiple of four times the decimation factor: the detector thresholds the image in tiles of that size, and starting the image off that grid moves every tile relative to the tag, which would shift the reported pose slightly when you turn the crop on.

A bound left sitting on the edge of the frame stays on the edge when the frame size changes: switch to a lower resolution and it shrinks to the new width or height, switch back up and it grows again. A bound you moved inside the frame keeps the pixel value you chose, and is only pulled in if the frame becomes too small to contain it.

This is useful for:

- **Ignoring irrelevant parts of the frame** — for example, cropping out the ceiling or a bumper that would otherwise produce false detections.
- **Increasing FPS** — processing fewer pixels reduces the per-frame workload, which can meaningfully raise the frame rate at high resolutions.

The crop is applied *after* orientation, so the crop region is defined in the coordinate space of the rotated image (the same image you see on the stream).

:::{note}
Camera calibration is automatically adjusted to account for the crop: the principal point is shifted by the crop origin while the focal lengths are preserved. This means 3D pose estimation (SolvePNP and AprilTag pose) remains accurate when a static crop is active -- a stationary tag reports the same pose whether the crop is on or off. Note, however, that any target lying outside the crop region can no longer be detected.
:::

## Stream Resolution

This changes the resolution which is used to stream frames from PhotonVision. This does not change the resolution used to perform vision processing. This is useful to reduce bandwidth consumption on the field. In some high-resolution cases, decreasing stream resolution can increase processing FPS.
