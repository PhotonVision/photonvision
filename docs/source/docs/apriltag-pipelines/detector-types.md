# AprilTag Pipeline Types

PhotonVision offers two different AprilTag pipeline types based on different implementations of the underlying algorithm. Each one has its advantages / disadvantages, which are detailed below.

:::{note}
Note that both of these pipeline types detect AprilTag markers and are just two different algorithms for doing so.
:::

## AprilTag

The AprilTag pipeline type is based on the [AprilTag](https://april.eecs.umich.edu/software/apriltag.html) library from the University of Michigan and we recommend it for most use cases. It is (to our understanding) most accurate pipeline type, but is also ~2x slower than AruCo. This was the pipeline type used by teams in the 2023 season and is well tested.

## AruCo

The AruCo pipeline is based on the [AruCo](https://docs.opencv.org/4.8.0/d9/d6a/group__aruco.html) library implementation from OpenCV. It is ~2x higher fps and ~2x lower latency than the AprilTag pipeline type, but is less accurate. We recommend this pipeline type for teams that need to run at a higher framerate or have a lower powered device. This pipeline type was new for the 2024 season.
