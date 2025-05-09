# Camera Calibration

:::{important}
In order to detect AprilTags and use 3D mode, your camera must be calibrated at the desired resolution! Inaccurate calibration will lead to poor performance.
:::

If you’re not using cameras in 3D mode, calibration is optional, but it can still offer benefits. Calibrating cameras helps refine the pitch and yaw values, leading to more accurate positional data in every mode. {ref}`For a more in-depth view<docs/calibration/calibration:Calibrating Your Camera>`.

## Print the Calibration Target

- Downloaded from our [demo site](http://photonvision.global/#/cameras), or directly from your coprocessors cameras tab.
- Use the Charuco calibration board:
  - Board Type: Charuco
  - Tag Family: 4x4
  - Pattern Spacing: 1.00in
  - Marker Size: 0.75in
  - Board Height : 8
  - Board Width : 8

## Prepare the Calibration Target

- Measure Accurately: Use calipers to measure the actual size of the squares and markers. Accurate measurements are crucial for effective calibration.
- Ensure Flatness: The calibration board must be perfectly flat, without any wrinkles or bends, to avoid introducing errors into the calibration process.

## Calibrate your Camera

- Take lots of photos: It's recommended to capture more than 50 images to properly calibrate your camera for accuracy. 12 is the bare minimum and may not provide good results.
- Other Tips
  - Move the board not the camera.
  - Take photos of lots of angles: The more angles the more better (up to 45 deg).
  - A couple of up close images is good.
  - Cover the entire cameras fov.
  - Avoid images with the board facing straight towards the camera.
