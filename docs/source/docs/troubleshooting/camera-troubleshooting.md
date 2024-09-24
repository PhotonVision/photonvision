# Camera Troubleshooting

## Pi Cameras

If you haven't yet, please refer to {ref}`the Pi CSI Camera Configuration page <docs/hardware/picamconfig:Pi Camera Configuration>` for information on updating {code}`config.txt` for your use case. If you've tried that, and things still aren't working, restart PhotonVision using the restart button in the settings tab, and press tilde (\`) in the web UI once connection is restored. This should show the most recent boot log.

|                                 | Expected output                                       | Bad                                |
| ------------------------------- | ----------------------------------------------------- | ---------------------------------- |
| LibCamera driver initialization | Successfully loaded libpicam shared object            | Failed to load native libraries!   |
| Camera detected                 | Adding local video device - "unicam" at "/dev/video0" | No output from VisionSourceManager |
| VisionSource created            | Adding 1 configs to VMM.                              | No output from VisionSourceManager |

If the driver isn't loaded, you may be using a non-official Pi image, or an image not new enough. Try updating to the most recent image available (one released for 2023) -- if that doesn't resolve the problem, {ref}`contact us<index:Contact Us>` with your settings ZIP file and Pi version/camera version/config.txt file used.

If the camera is not detected, the most likely cause is either a config.txt file incorrectly set-up, or a ribbon cable attached backwards. Review the {ref}`picam configuration page <docs/hardware/picamconfig:pi camera configuration>`, and verify the ribbon cable is properly oriented at both ends, and that it is \_fully\_ inserted into the FFC connector. Then, {ref}`contact us<index:Contact Us>` with your settings ZIP file and Pi version/camera version/config.txt file used.

## USB cameras

USB cameras supported by CSCore require no libcamera driver initialization to work -- however, similar troubleshooting steps apply. Restart PhotonVision using the restart button in the settings tab, and press tilde on your keyboard (\`) when you're in the web UI once connection is restored. We expect to see the following output:

|                      | Expected output                                       | Bad                                |
| -------------------- | ----------------------------------------------------- | ---------------------------------- |
| Camera detected      | Adding local video device - "foobar" at "/dev/foobar" | No output from VisionSourceManager |
| VisionSource created | Adding 1 configs to VMM.                              | No output from VisionSourceManager |

## Determining detected cameras in Video4Linux (v4l2)

On Linux devices (including Raspberry Pi), PhotonVision uses WPILib's CSCore to interact with video devices, which internally uses Video4Linux (v4l2). CSCore, and therefore Photon, requires that cameras attached have good v4l drivers for proper functionality. These should be built into the Linux kernel, and do not need to be installed manually. Valid picamera setup (from /boot/config.txt) can also be determined using these steps. The list-devices command will show all valid video devices detected, and list-formats the list of "video modes" each camera can be in.

- For picams: edit the config.txt file as described in the {ref}`picam configuration page <docs/hardware/picamconfig:pi camera configuration>`
- SSH into your Pi: {code}`ssh pi@photonvision.local` and enter the username "pi" & password "raspberry"
- run {code}`v4l2-ctl --list-devices` and {code}`v4l2-ctl --list-formats`

We expect an output similar to the following. For picameras, note the "unicam" entry with path {code}`platform:3f801000.csi` (if we don't see this, that's bad), and a huge list of valid video formats. USB cameras should show up similarly in the output of these commands.

```{eval-rst}
.. tab-set::
  .. tab-item:: Working

        .. code-block::

            pi@photonvision:~ $ v4l2-ctl --list-devices
            unicam (platform:3f801000.csi):
                /dev/video0
                /dev/media3

            bcm2835-codec-decode (platform:bcm2835-codec):
                /dev/video10
                /dev/video11
                /dev/video12
                /dev/video18
                /dev/video31
                /dev/media2

            bcm2835-isp (platform:bcm2835-isp):
                /dev/video13
                /dev/video14
                /dev/video15
                /dev/video16
                /dev/video20
                /dev/video21
                /dev/video22
                /dev/video23
                /dev/media0
                /dev/media1

            pi@photonvision:~ $ v4l2-ctl --list-formats
            ioctl: VIDIOC_ENUM_FMT
            Type: Video Capture

            [0]: 'YUYV' (YUYV 4:2:2)
            [1]: 'UYVY' (UYVY 4:2:2)
            [2]: 'YVYU' (YVYU 4:2:2)
            [3]: 'VYUY' (VYUY 4:2:2)
            <snip>
            [42]: 'Y12P' (12-bit Greyscale (MIPI Packed))
            [43]: 'Y12 ' (12-bit Greyscale)
            [44]: 'Y14P' (14-bit Greyscale (MIPI Packed))
            [45]: 'Y14 ' (14-bit Greyscale)

  .. tab-item:: Not Working

        .. code-block::

            pi@photonvision:~ $ v4l2-ctl --list-devices
            bcm2835-codec-decode (platform:bcm2835-codec):
                /dev/video10
                /dev/video11
                /dev/video12
                /dev/video18
                /dev/video31
                /dev/media3
             bcm2835-isp (platform:bcm2835-isp):
                /dev/video13
                /dev/video14
                /dev/video15
                /dev/video16
                /dev/video20
                /dev/video21
                /dev/video22
                /dev/video23
                /dev/media0
                /dev/media1
            rpivid (platform:rpivid):
                /dev/video19
                /dev/media2
            Cannot open device /dev/video0, exiting.
```
