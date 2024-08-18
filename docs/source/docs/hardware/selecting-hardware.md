# Selecting Hardware

In order to use PhotonVision, you need a coprocessor and a camera. This page will help you select the right hardware for your team depending on your budget, needs, and experience.

## Choosing a Coprocessor

### Minimum System Requirements

- Ubuntu 22.04 LTS or Windows 10/11
  - We don't recommend using Windows for anything except testing out the system on a local machine.
- CPU: ARM Cortex-A53 (the CPU on Raspberry Pi 3) or better
- At least 8GB of storage
- 2GB of RAM
    - PhotonVision isn't very RAM intensive, but you'll need at least 2GB to run the OS and PhotonVision.
- The following IO:
    - At least 1 USB or MIPI-CSI port for the camera
        - Note that we only support using the Raspberry Pi's MIPI-CSI port, other MIPI-CSI ports from other coprocessors may not work.
    - Ethernet port for networking

### Coprocessor Recommendations

When selecting a coprocessor, it is important to consider various factors, particularly when it comes to AprilTag detection. Opting for a coprocessor with a more powerful CPU can generally result in higher FPS AprilTag detection, leading to more accurate pose estimation. However, it is important to note that there is a point of diminishing returns, where the benefits of a more powerful CPU may not outweigh the additional cost. Below is a list of supported hardware, along with some notes on each.

- Orange Pi 5 (\$99)
    - This is the recommended coprocessor for most teams. It has a powerful CPU that can handle AprilTag detection at high FPS, and is relatively cheap compared to processors of a similar power.
- Raspberry Pi 4/5 (\$55-\$80)
    - This is the recommended coprocessor for teams on a budget. It has a less powerful CPU than the Orange Pi 5, but is still capable of running PhotonVision at a reasonable FPS.
- Mini PCs (such as Beelink N5095)
    - This coprocessor will likely have similar performance to the Orange Pi 5 but has a higher performance ceiling (when using more powerful CPUs). Do note that this would require extra effort to wire to the robot / get set up. More information can be found in the set up guide [here.](https://docs.google.com/document/d/1lOSzG8iNE43cK-PgJDDzbwtf6ASyf4vbW8lQuFswxzw/edit?usp=drivesdk)
- Other coprocessors can be used but may require some extra work / command line usage in order to get it working properly.

## Choosing a Camera

PhotonVision works with Pi Cameras and most USB Cameras, the recommendations below are known to be working and have been tested. Other cameras such as webcams, virtual cameras, etc. are not officially supported and may not work. It is important to note that fisheye cameras should only be used as a driver camera and not for detecting targets.

PhotonVision relies on [CSCore](https://github.com/wpilibsuite/allwpilib/tree/main/cscore) to detect and process cameras, so camera support is determined based off compatibility with CScore along with native support for the camera within your OS (ex. [V4L compatibility](https://en.wikipedia.org/wiki/Video4Linux) if using a Linux machine like a Raspberry Pi).

:::{note}
Logitech Cameras and integrated laptop cameras will not work with PhotonVision due to oddities with their drivers. We recommend using a different camera.
:::

:::{note}
We do not currently support the usage of two of the same camera on the same coprocessor. You can only use two or more cameras if they are of different models or they are from Arducam, which has a [tool that allows for cameras to be renamed](https://docs.arducam.com/UVC-Camera/Serial-Number-Tool-Guide/).
:::

### Recommended Cameras

For colored shape detection, any non-fisheye camera supported by PhotonVision will work. We recommend the Pi Camera V1 or a high fps USB camera.

For driver camera, we recommend a USB camera with a fisheye lens, so your driver can see more of the field.

For AprilTag detection, we recommend you use a global shutter camera that has ~100 degree diagonal FOV. This will allow you to see more AprilTags in frame, and will allow for more accurate pose estimation. You also want a camera that supports high FPS, as this will allow you to update your pose estimator at a higher frequency.

- Recommendations For AprilTag Detection
    - Arducam USB OV9281
        - This is the recommended camera for AprilTag detection as it is a high FPS, global shutter camera USB camera that has a ~70 degree FOV.
    - Innomaker OV9281
    - Spinel AR0144
    - Pi Camera Module V1
        - The V1 is strongly preferred over the V2 due to the V2 having undesirable FOV choices

### AprilTags and Motion Blur

When detecting AprilTags, you want to reduce the "motion blur" as much as possible. Motion blur is the visual streaking/smearing on the camera stream as a result of movement of the camera or object of focus. You want to mitigate this as much as possible because your robot is constantly moving and you want to be able to read as many tags as you possibly can. The possible solutions to this include:

1. Cranking your exposure as low as it goes and increasing your gain/brightness. This will decrease the effects of motion blur and increase FPS.
2. Using a global shutter (as opposed to rolling shutter) camera. This should eliminate most, if not all motion blur.
3. Only rely on tags when not moving.

```{image} images/motionblur.gif
:align: center
```

### Using Multiple Cameras

Using multiple cameras on your robot will help you detect more AprilTags at once and improve your pose estimation as a result. In order to use multiple cameras, you will need to create multiple PhotonPoseEstimators and add all of their measurements to a single drivetrain pose estimator. Please note that the accuracy of your robot to camera transform is especially important when using multiple cameras as any error in the transform will cause your pose estimations to "fight" each other. For more information, see {ref}`the programming reference. <docs/programming/index:programming reference>`.

## Performance Matrix

```{raw} html
<embed>

    <iframe src="https://docs.google.com/spreadsheets/d/e/2PACX-1vTojOew2d2NQY4PRA98vjkS1ECZ2YNvods-aOdk2x-Q4aF_7r4mcwlyTe8GjUKmUxEiVgGNnJNhEdyd/pubhtml?gid=1779881081&amp;single=true&amp;widget=true&amp;headers=false" width="760" height="500" frameborder="0" marginheight="0" marginwidth="0">Loading…</iframe>

</embed>
```

Please submit performance data to be added to the matrix here:

```{raw} html
<embed>

    <iframe src="https://docs.google.com/forms/d/e/1FAIpQLSf5iK3pX0Tn8bxpRYgcTAy4scUu14rUvJqkTyfzoKc-GiV7Vg/viewform?embedded=true" width="760" height="500" frameborder="0" marginheight="0" marginwidth="0">Loading…</iframe>

</embed>
```
