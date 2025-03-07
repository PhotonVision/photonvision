# Latency Characterization


## A primer on time

Especially starting around 2022 with AprilTags making localization easier, providing a way to know when a camera image was captured at became more important for localization.
Since the [creation of USBFrameProvider](https://github.com/PhotonVision/photonvision/commit/f92bf670ded52b59a00352a4a49c277f01bae305), we used the time [provided by CSCore](https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/cscore/CvSink.html#grabFrame(org.opencv.core.Mat)) to tell when a camera image was captured at, but just keeping track of "CSCore told us frame N was captured 104.21s after the Raspberry Pi turned on" isn't very helpful. We can decompose this into asking:

- At what time was a particular image captured at, in the coprocessor's timebase?
- How do I convert a time in a coprocessor's timebase into the RoboRIO's timebase, so I can integrate the measurement with my other sensor measurements (like encoders)?

The first one seems easy - CSCore tells us the time, so just keep track of that? Should be easy. For the second, translating this time, as measured by the coprocessor's clock, into a timebase also used by user code on the RoboRIO, is actually a [fairly hard problem](time-sync.md) that involved reinventing [PTP](https://en.wikipedia.org/wiki/PTP).

And on latency vs timestamps - PhotonVision has exposed a magic "latency" number since forever, but latency (as in, the time from image capture to acting on data) can be useful for benchmarking code, but robots actually want to answer "what time was this image from, relative to "?


## CSCore's Frame Time

WPILib's CSCore is a platform-agnostic wrapper around Windows, Linux, and MacOS camera APIs. On Linux, CSCore uses [Video4Linux](https://en.wikipedia.org/wiki/Video4Linux) to access USB Video Class (UVC) devices like webcams, as well as CSI cameras on some platforms. At a high level, CSCore's [Linux USB Camera driver](https://github.com/wpilibsuite/allwpilib/blob/17a03514bad6de195639634b3d57d5ac411d601e/cscore/src/main/native/linux/UsbCameraImpl.cpp) works by:

- Opening a camera with `open`
- Creating and `mmap`ing a handful of buffers V4L will fill with frame data into program memory
- Asking V4L to start streaming
- While the camera is running:
  - Wait for new frames
  - Dequeue one buffer
  - Call `SourceImpl::PutFrame`, which will copy the image out and convert as needed
  - Return the buffer to V4L to fill again

Prior to https://github.com/wpilibsuite/allwpilib/pull/7609, CSCore used the [time it dequeued the buffer at](https://github.com/wpilibsuite/allwpilib/blob/17a03514bad6de195639634b3d57d5ac411d601e/cscore/src/main/native/linux/UsbCameraImpl.cpp#L559) as the image capture time. But this doesn't account for exposure time or latency introduced by the camera + USB stack + Linux itself.

V4L does expose (with some [very heavy caveats](https://github.com/torvalds/linux/blob/fc033cf25e612e840e545f8d5ad2edd6ba613ed5/drivers/media/usb/uvc/uvc_video.c#L600) for some troublesome cameras) its best guess at the time an image was captured at via [buffer flags](https://www.kernel.org/doc/html/v4.9/media/uapi/v4l/buffer.html#buffer-flags). In my testing, all my cameras were able to provide timestamps with both these flags set:
- `V4L2_BUF_FLAG_TIMESTAMP_MONOTONIC`: The buffer timestamp has been taken from the CLOCK_MONOTONIC clock [...] accessible via `clock_gettime()`.
- `V4L2_BUF_FLAG_TSTAMP_SRC_SOE`: Start Of Exposure. The buffer timestamp has been taken when the exposure of the frame has begun.

I'm sure that we'll find a camera that doesn't play nice, because we can't have nice things :). But until then, using this timestamp gets us a free accuracy bump.

Other things to note: This gets us an estimate at when the camera *started* collecting photons. The camera's sensor will remain collecting light for up to the total integration time, plus readout time for rolling shutter cameras.

## Latency Testing

Here, I've got a RoboRIO with an LED, an Orange Pi 5, and a network switch on a test bench. The LED is assumed to turn on basically instantly once we apply current, and based on DMA testing, the total time to switch a digital output on is on the order of 10uS. The RoboRIO is running a TimeSync Server, and the Orange Pi is running a TimeSync Client.

### Test Setup

<details>
<summary>Show RoboRIO Test Code</summary>

```java
package frc.robot;

import org.photonvision.PhotonCamera;

import edu.wpi.first.wpilibj.DigitalOutput;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends TimedRobot {
    PhotonCamera camera;
    DigitalOutput light;

    @Override
    public void robotInit() {
        camera = new PhotonCamera("Arducam_OV9782_USB_Camera");

        light = new DigitalOutput(0);
        light.set(false);
    }

    @Override
    public void robotPeriodic() {
        super.robotPeriodic();

        try {
            light.set(false);
            for (int i = 0; i < 50; i++) {
                Thread.sleep(20);
                camera.getAllUnreadResults();
            }

            var t1 = Timer.getFPGATimestamp();
            light.set(true);
            var t2 = Timer.getFPGATimestamp();


            for (int i = 0; i < 100; i++) {
                for (var result : camera.getAllUnreadResults()) {
                    if (result.hasTargets()) {
                        var t3 = result.getTimestampSeconds();
                        var t1p5 = (t1 + t2) / 2;
                        var error = t3-t1p5;
                        SmartDashboard.putNumber("blink_error_ms", error * 1000);
                        return;
                    }
                }

                Thread.sleep(20);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```
</details>

I've decreased camera exposure as much as possible (so we know with reasonable confidence that the image was collected right at the start of the exposure time reported by V4L), but we only get back new images at 60fps. So we don't know when between frame N and N+1 the LED turned on - just that sometime between now and 1/60th of a second a go, the LED turned on.

The test coprocessor was an Orange Pi 5 running a PhotonVision 2025 (Ubuntu 24.04 based) image, with an ArduCam OV9782 at 1280x800, 60fps, MJPG running a reflective pipeline.


### Test Results

The videos above show the difference between when the RoboRIO turned the LED on and when PhotonVision first seeing a camera frame with the LED on, what I've called error and plotted in yellow with units of seconds. This error decreases when I use the frame time reported by V4L from a mean delta of 26 ms to a mean delta of 11 ms (below the maximum temporal resolution of my camera).

Old CSCore:
```{raw} html
<video width="85%" controls>
    <source src="../../../_static/assets/latency-tests/ov9782_1280x720x60xMJPG_old.mp4" type="video/mp4">
    Your browser does not support the video tag.
</video>
```
CSCore using V4L frame time:
```{raw} html
<video width="85%" controls>
    <source src="../../../_static/assets/latency-tests/ov9782_1280x720x60xMJPG_new.mp4" type="video/mp4">
    Your browser does not support the video tag.
</video>
```

With the camera capturing at 60fps, the time between successive frames is only ~16.7 ms, so I don't expect to be able to resolve anything smaller. Given sufficient time and with perfect latency compensation, and with more noise in the robot program to make sure we vary LED toggle times, I'd expect the error to converge to ~half the interval between frames - so being within this frame interval with CSCore updates is a very good sign.

### Future Work

This test also makes no effort to isolate error from time synchronization from error introduced by frame time measurement - we're just interested in overall error. Future work could investigate the latency contribution
