# Selecting Hardware

In order to use PhotonVision, you need a coprocessor and a camera. Other than the recommended hardware found in the quick start guide, this page will help you select hardware that should work for photonvision even though it is not supported/recommended.

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
    - Note that we only support using the Raspberry Pi's MIPI-CSI port, other MIPI-CSI ports from other coprocessors will probably not work.
  - Ethernet port for networking

### Coprocessor Recommendations

When selecting a coprocessor, it is important to consider various factors, particularly when it comes to AprilTag detection. Opting for a coprocessor with a more powerful CPU can generally result in higher FPS AprilTag detection, leading to more accurate pose estimation. However, it is important to note that there is a point of diminishing returns, where the benefits of a more powerful CPU may not outweigh the additional cost. Other coprocessors can be used but may require some extra work / command line usage in order to get it working properly.

## Choosing a Camera

PhotonVision works with Pi Cameras and most USB Cameras. Other cameras such as webcams, virtual cameras, etc. are not officially supported and may not work. It is important to note that fisheye cameras should only be used as a driver camera / gamepeice detection and not for detecting targets / apriltags.

PhotonVision relies on [CSCore](https://github.com/wpilibsuite/allwpilib/tree/main/cscore) to detect and process cameras, so camera support is determined based off compatibility with CScore along with native support for the camera within your OS (ex. [V4L compatibility](https://en.wikipedia.org/wiki/Video4Linux) if using a Linux machine like a Raspberry Pi).

:::{note}
Logitech Cameras and integrated laptop cameras will not work with PhotonVision due to oddities with their drivers. We recommend using a different camera.
:::

:::{note}
We do not currently support the usage of two of the same camera on the same coprocessor. You can only use two or more cameras if they are of different models or they are from Arducam, which has a [tool that allows for cameras to be renamed](https://docs.arducam.com/UVC-Camera/Serial-Number-Tool-Guide/).
:::

### Cameras Attributes

For colored shape detection, any non-fisheye camera supported by PhotonVision will work. We recommend a high fps USB camera.

For driver camera, we recommend a USB camera with a fisheye lens, so your driver can see more of the field.

For AprilTag detection, we recommend you use a global shutter camera that has ~100 degree diagonal FOV. This will allow you to see more AprilTags in frame, and will allow for more accurate pose estimation. You also want a camera that supports high FPS, as this will allow you to update your pose estimator at a higher frequency.

Another cause of image distortion is 'rolling shutter.' This occurs when the camera captures pixels sequentially from top to bottom, which can also lead to distortion if the camera or object is moving.

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
