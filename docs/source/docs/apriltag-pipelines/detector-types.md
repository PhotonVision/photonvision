# AprilTag Pipeline Types

PhotonVision offers three different AprilTag pipeline types based on different implementations of the underlying algorithm. Each one has its advantages / disadvantages, which are detailed below.

:::{note}
Note that all of these pipeline types detect AprilTag markers and are just different algorithms for doing so.
:::

## AprilTag

The AprilTag pipeline type is based on the [AprilTag](https://april.eecs.umich.edu/software/apriltag.html) library from the University of Michigan and we recommend it for most use cases. It is (to our understanding) most accurate pipeline type, but is also ~2x slower than ArUco. This was the pipeline type used by teams in the 2023 season and is well tested.

## ArUco

The ArUco pipeline is based on the [ArUco](https://docs.opencv.org/4.8.0/d9/d6a/group__aruco.html) library implementation from OpenCV. It is ~2x higher fps and ~2x lower latency than the AprilTag pipeline type, but is less accurate. We recommend this pipeline type for teams that need to run at a higher framerate or have a lower powered device. This pipeline type was new for the 2024 season.

## AprilTag (Vulkan) - Beta

:::{warning}
This pipeline type is **beta** and may be removed in a future release if it can't be kept up to date. It has no guaranteed support window.
:::

The Vulkan AprilTag pipeline runs the same [AprilTag](https://april.eecs.umich.edu/software/apriltag.html) decoding algorithm as the standard AprilTag pipeline, but does the GPU-parallelizable stages of detection (decimation, thresholding, connected-component labeling, and boundary extraction) as Vulkan compute shaders instead of on the CPU, via the [vkapriltag](https://github.com/yojobama/vkapriltag) library. This targets low-powered ARM single-board computers with a capable GPU (e.g. the Orange Pi 5's Mali G610) where the CPU AprilTag pipeline is the bottleneck and a CUDA-based accelerator isn't an option.

Requirements and known limitations:

- Requires a Vulkan 1.1+ capable GPU and driver. Only `linuxarm64` and `linuxx86-64` builds include the native detector at all.
- **Falls back to the CPU AprilTag detector automatically** if Vulkan isn't available, the requested camera resolution isn't evenly divisible by the chosen `Decimation` factor, or the native detector otherwise fails to initialize. There is no separate indication in the UI today when this fallback is active beyond the resulting frame rate.
- A `Decimation` control (1x/2x/4x, default 2x for continuity with earlier versions of this pipeline) is available, as of vkapriltag v1.3.0 - lower values are more accurate but slower; the camera's resolution must be evenly divisible by whichever value is chosen.
- A `Refine Edges` control is available as of vkapriltag v1.4.1, defaulting to off. It runs upstream libapriltag's own gradient-based corner refinement on each detected quad, so enabling it matches the standard AprilTag pipeline's behavior rather than approximating it. It defaults to off because it is expensive: vkapriltag's own profiling puts that refinement at roughly half of all CPU cycles in the pipeline, GPU phases included. As of vkapriltag v1.5.0 the implementation used by default is a rewrite of that refinement that is bit-identical to upstream's by construction, so it is cheaper without changing results - but it remains a speed-versus-corner-precision choice, not a free one.
- This pipeline type still has no `Blur` control, unlike the standard AprilTag pipeline: vkapriltag has no Gaussian pre-blur stage. That isn't configurable because it isn't implemented, not because it's hidden.
- A device picker lets you choose a specific GPU when more than one Vulkan-capable device is present; "Automatic" defers to the library's own scoring (discrete GPU > integrated GPU > virtual GPU), which is a reasonable default on almost every coprocessor.
- A `Pose Estimator` control lets you choose between WPILib's CPU pose estimator (the well-tested default) and an **experimental** Vulkan-native one. The Vulkan-native estimator is measured much faster per tag than unmodified upstream libapriltag, but that comparison is not against WPILib's own implementation (what this pipeline actually uses by default), so its real-world benefit here is unverified - compare results carefully before relying on it. Multi-tag pose estimation is unaffected by this choice either way.
