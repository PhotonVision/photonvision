# Common Issues / Questions

This page will grow as needed in order to cover commonly seen issues by teams. If this page doesn't help you and you need further assistance, feel free to {ref}`Contact Us<index:Contact Us>`.

## Known Issues

All known issues can be found on our [GitHub page](https://github.com/PhotonVision/photonvision/issues).

### PS3Eye

Due to an issue with Linux kernels, the drivers for the PS3Eye are no longer supported. If you would still like to use the PS3Eye, you can downgrade your kernel with the following command: `sudo CURL_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt rpi-update 866751bfd023e72bd96a8225cf567e03c334ecc4`. Note: You must be connected to the internet to run the command.

### LED Control

The logic for controlling LED mode when `multiple cameras are connected` is not fully fleshed out. In its current state, LED control is only enabled when a Pi Camera Module is not in driver mode—meaning a USB camera on its own is unable to control the LEDs.

For now, if you are using multiple cameras, it is recommended that teams set the value of the NetworkTables entry {code}`photonvision/ledMode` from the robot code to control LED state.

## Commonly Seen Issues

### Networking Issues

Please refer to our comprehensive {ref}`networking troubleshooting tips <docs/troubleshooting/networking-troubleshooting:Networking Troubleshooting>` for debugging suggestions and possible causes.

### Camera won't show up

Try these steps to {ref}`troubleshoot your camera connection <docs/troubleshooting/camera-troubleshooting:Camera Troubleshooting>`.

If you are using a USB camera, it is possible your USB Camera isn't supported by CSCore and therefore won't work with PhotonVision.

### Camera is consistently returning incorrect values when in 3D mode

Read the tips on the {ref}`camera calibration page<docs/calibration/calibration:Calibration Tips>`, follow the advice there, and redo the calibration.

### Not getting data from PhotonLib

1. Ensure your coprocessor version and PhotonLib version match. This can be checked by the settings tab and examining the .json itself (respectively).
2. Ensure that you have your team number set properly.
3. Use Glass to verify that PhotonVision has connected to the NetworkTables server served by your robot. With Glass connected in client mode to your RoboRIO, we expect to see "photonvision" listed under the Clients tab of the NetworkTables Info pane.

```{image} images/glass-connections.png
:alt: Using Glass to check NT connections
:width: 600
```

4. When creating a `PhotonCamera` in code, does the `cameraName` provided match the name in the upper-right card of the web interface? Glass can be used to verify the RoboRIO is receiving NetworkTables data by inspecting the `photonvision` subtable for your camera nickname.

```{image} images/camera-subtable.png
:alt: Using Glass to check camera publishing
:width: 600
```

### Unable to download PhotonLib

Ensure all of your network firewalls are disabled and you aren't on a school-network.

### PhotonVision prompts for login on startup

This is normal. You don't need to connect a display to your Raspberry Pi to use PhotonVision, just navigate to the relevant webpage (ex. `photonvision.local:5800`) in order to see the dashboard.

### Raspberry Pi enters into boot looping state when using PhotonVision

This is most commonly seen when your Pi doesn't have adequate power / is being undervolted. Ensure that your power supply is functioning properly.
