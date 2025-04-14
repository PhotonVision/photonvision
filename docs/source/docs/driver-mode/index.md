# Driver Mode

Driver Mode is a special type of pipeline that allows PhotonVision to forward a camera's stream to NetworkTables. The stream can be viewed from a driver dashboard, and is accessible under the `CameraPublisher` NT field. This is useful for situations where you want to use the camera for driver assistance in teleop.

## Enabling Driver Mode

To enable Driver Mode, toggle the switch at the top of the Dashboard page for a selected camera. 

Alternatively, visit the camera settings page and toggle the "Driver Mode" switch for a selected camera.

## Hiding the Crosshair
When Driver Mode is enabled, the camera stream will show a green crosshair at the center of the camera stream. If you do not want to show the green crosshair at the center of the camera stream, toggle the "Crosshair" switch under the Input tab, as shown in the image below.
