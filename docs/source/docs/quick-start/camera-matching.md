# Camera Matching

## Activating and Deactivating Cameras

When you first plug in a camera, it will be detected and added to the list of cameras with the "Unassigned" status, as shown below. You can press the "Activate" button to enable PhotonVision to use the camera.

```{image} images/camera-matching/unassigned-camera.png
:scale: 50%
```

If a camera has been activated in the past, it will be listed as "Deactivated" in the camera list. You can press the "Activate" button to enable PhotonVision to use the camera.

```{image} images/camera-matching/deactivated-camera.png
:scale: 50%
```

Once a camera is activated, it will be listed as "Active" in the camera list. You can press the "Deactivate" button to stop PhotonVision from using the camera.

```{image} images/camera-matching/activated-camera.png
:scale: 50%
```

## Deleting Cameras

If you want to remove a camera from the list, you can press the delete button. This will clear all settings for that particular camera, including the calibration data and any other settings you have configured. It is recommended to make a backup of the camera's settings before deleting it, as this action cannot be undone.

## Camera Matching

When you plug in a camera, PhotonVision will attempt to match it to a previously configured camera based on the physical USB port it is connected to. If you plug another camera into that port, the cameras will have a "Camera Mismatch" status, indicating that the camera is not recognized as the one that was previously configured.

Additionally, pressing on the Details button will show you the details of the camera mismatch, allowing you to compare the current camera with the previously configured camera.

```{image} images/camera-matching/camera-mismatch-details.png
:scale: 50%
```

```{note}
Camera matching is based on the USB ports on the device. If you unplug a camera and plug it into a different port, PhotonVision will attempt to use settings from the camera that was previously configured in that port, causing unexpected behavior.
```

To resolve the camera mismatch, you should ensure each camera is plugged into the same port that you configured it in.
