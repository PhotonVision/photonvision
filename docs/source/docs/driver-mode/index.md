# Driver Mode

Driver Mode is a type of pipeline that allows PhotonVision to forward a camera's stream to NetworkTables without running any vision processing. The stream can be viewed from a driver dashboard, and is accessible under the `CameraPublisher` NT table. This is useful for situations where you want to use the camera for driver assistance in teleop.

:::{note}
The only subtable under `CameraPublisher` that will work for viewing a driver mode camera stream is the one that contains `Output` in the name.
:::

## Enabling Driver Mode

To enable Driver Mode, toggle the switch at the top of the Dashboard page for a selected camera. 

```{image} images/driver-mode-dashboard.png
:align: center
:alt: Driver Mode Toggle in the Dashboard Page
```

Alternatively, visit the camera settings page and toggle the "Driver Mode" switch for a selected camera.

```{image} images/driver-mode-camera-settings.png
:align: center
:alt: Driver Mode Toggle in the Camera Settings Page
```

## Hiding the Crosshair
When Driver Mode is enabled, the camera stream will show a green crosshair at the center of the camera stream. If you do not want to show the green crosshair at the center of the camera stream, toggle the "Crosshair" switch under the Input tab, as shown in the image below.

```{image} images/crosshair-switch.png
:align: center
:alt: Crosshair Switch
```