# Selecting Hardware

:::{note}
See the {ref}`quick start guide<docs/quick-start/common-setups:Common Hardware Setups>`, for latest, specific recommendations on hardware to use for PhotonVision.
:::

In order to use PhotonVision, you need a coprocessor and a camera. This page discusses the specifics of why that hardware is recommended.

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

Note these are bare minimums. Most high-performance vision processing will require higher specs.

### Coprocessor Recommendations

Vision processing on one camera stream is usually a CPU-bound operation. Some operations are able to be done in parallel, but not all. USB bandwidth and network data transfer also cause a fixed overhead.

Faster CPU's generally result in lower latency, but eventually with diminishing returns. More cores allow for some improvement, especially if multiple camera streams are being processed.

PhotonVision is most commonly tested around Raspbian (Debian-based) operating systems.

Other coprocessors can be used but may require some extra work / command line usage in order to get it working properly.

### Power Supply

Coprocessors need a steady, regulated power supply. Under-volting the processor will result in CPU throttling, low performance, unexpected reboots, and sometimes electrical damage. Many coprocessors draw 5-10 amps of current.

Be sure to select a power supply which regulate's the robot's variable battery voltage into something steady that the robot can use.

### Storage Media

Most single-board computer coprocessors use micro SD cards as their storage media.

Three important considerations include total storage space, read/write speed, and robustness.

PhotonVision is not usually disk-bound, other than during coprocessor boot-up and initial startup. Some disk writing is done at runtime for logging, settings, and saving camera images on command.

Better storage space and read/write speed mostly matter if image capture is used frequently on the field.

Industrial-grade SD cards are recommended for their stability under shock, vibration, variable voltage, and power-off. Raspberry Pi and Orange Pi coprocessors are generally robust against robot power interruptions, teams have anecdotally reported that Sandisk industrial SD cards reduce the chances of an unexpected settings or log file corruption on shutdown.


## Choosing a Camera

PhotonVision relies on [CSCore](https://github.com/wpilibsuite/allwpilib/tree/main/cscore) to detect and process cameras, so camera support is determined based off compatibility with CScore along with native support for the camera within your OS (ex. [V4L compatibility](https://en.wikipedia.org/wiki/Video4Linux)).

PhotonVision attempts to support most USB Cameras. Exceptions include:

- All Logitech brand cameras
  - Logitech uses a non-standard driver which is not currently supported
- Built-in webcams
  - Driver support is too varied. Some may happen to work, but most have been found to be non-functional
- virtual cameras (OBS, Snapchat camera, etc.)
  - PhotonVision assumes the camera has real physical hardware to control - these do not expose the minimum number of controls.

Use caution when using multiple identical cameras, as only the physical USB port they are plugged into can differentiate them. PhotonVision provides a "strict matching" setting which can reduce errors related to identical cameras. Arducam has a [tool that allows for identical cameras to be renamed](https://docs.arducam.com/UVC-Camera/Serial-Number-Tool-Guide/) by their physical location or purpose.


### Cameras Attributes

For colored shape detection, any non-fisheye camera supported by PhotonVision will work.

For driver camera, we recommend a USB camera with a fisheye lens, so your driver can see more of the field. Use the minimum acceptable resolution to help keep latency low.

For AprilTag detection, we recommend you use a camera that has ~100 degree diagonal FOV. This will allow you to see more AprilTags in frame, and will allow for more accurate pose estimation. You also want a camera that supports high FPS, as this will allow you to update your pose estimator at a higher frequency.

For object detection, we recommend a USB camera. Some fisheye lenses may be ok, but very wide angle cameras may distort the gamepiece beyond recognition.

Global shutter cameras are recommended in all cases, to reduce rolling-shutter image sheer while the robot is moving.

```{image} images/rollingshutter.gif
:align: center
```

Cameras capable of capturing a good image with very short exposures will also help reduce image blur. Usually, high-FPS-capable cameras designed for computer vision are better at this than "consumer-grade" USB webcams.

### Using Multiple Cameras

Keeping the target(s) in view of the robot often requires more than one camera. PhotonVision has no hardcoded limit on the number of cameras supported. The limit is usually dependant on CPU (can all frames be processed fast enough?) and USB bandwidth (Can all cameras send their images without overwhelming the bus?).

Note that cameras are not synchronized together. Frames are captured and processed asynchronously. Robot Code must fuse estimates together. For more information, see {ref}`the programming reference. <docs/programming/index:programming reference>`.

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
