# Simulation Support in PhotonLib in C++

## What Is Simulated?

Simulation is a powerful tool for validating robot code without access to a physical robot. Read more about [simulation in WPILib](https://docs.wpilib.org/en/stable/docs/software/wpilib-tools/robot-simulation/introduction.html).

In C++, PhotonLib can simulate cameras on the field and generate target data approximating what would be seen in reality. This simulation attempts to include the following:

- Camera Properties
  - Field of Vision
  - Lens distortion
  - Image noise
  - Framerate
  - Latency
- Target Data
  - Detected / minimum-area-rectangle corners
  - Center yaw/pitch
  - Contour image area percentage
  - Fiducial ID
  - Fiducial ambiguity
  - Fiducial solvePNP transform estimation
- Camera Raw/Processed Streams (grayscale)

:::{note}
Simulation does NOT include the following:

- Full physical camera/world simulation (targets are automatically thresholded)
- Image Thresholding Process (camera gain, brightness, etc)
- Pipeline switching
- Snapshots
:::

This scope was chosen to balance fidelity of the simulation with the ease of setup, in a way that would best benefit most teams.

```{image} diagrams/SimArchitecture.drawio.svg
:alt: A diagram comparing the architecture of a real PhotonVision process to a simulated
:  one.
```

## Drivetrain Simulation Prerequisite

A prerequisite for simulating vision frames is knowing where the camera is on the field-- to utilize PhotonVision simulation, you'll need to supply the simulated robot pose periodically. This requires drivetrain simulation for your robot project if you want to generate camera frames as your robot moves around the field.

References for using PhotonVision simulation with drivetrain simulation can be found in the [PhotonLib C++ Examples](https://github.com/PhotonVision/photonvision/tree/main/photonlib-cpp-examples) for both a differential drivetrain and a swerve drive. `photonlib-cpp-examples/aimandrange/src/main/include/VisionSim.h` is the file that wires the pieces below together.

:::{important}
The simulated drivetrain pose must be separate from the drivetrain estimated pose if a pose estimator is utilized.
:::

## Vision System Simulation

A `photon::VisionSystemSim` represents the simulated world for one or more cameras, and contains the vision targets they can see. It is constructed with a unique label:

```cpp
// A vision system sim labelled as "main" in NetworkTables
photon::VisionSystemSim visionSim{"main"};
```

The examples below assume `<photon/simulation/VisionSystemSim.h>` and `<photon/simulation/VisionTargetSim.h>` are included.

PhotonLib will use this label to put a `Field2d` widget on NetworkTables at `/VisionSystemSim-[label]/Sim Field`. This label does not need to match any camera name or pipeline name in PhotonVision.

Vision targets require a `photon::TargetModel`, which describes the shape of the target. For AprilTags, PhotonLib provides `photon::kAprilTag16h5` for the tags used in 2023, and `photon::kAprilTag36h11` for the tags used starting in 2024. For other target shapes, convenience constructors exist for spheres, cuboids, and planar rectangles. For example, a planar rectangle can be created with:

```cpp
// A 0.5 x 0.25 meter rectangular target
photon::TargetModel targetModel{0.5_m, 0.25_m};
```

These `TargetModel` are paired with a target pose to create a `photon::VisionTargetSim`. A `VisionTargetSim` is added to the `VisionSystemSim` to become visible to all of its cameras.

```cpp
// The pose of where the target is on the field.
// Its rotation determines where "forward" or the target x-axis points.
// Let's say this target is flat against the far wall center, facing the blue driver stations.
wpi::math::Pose3d targetPose{16_m, 4_m, 2_m, wpi::math::Rotation3d{0_deg, 0_deg, 180_deg}};
// The given target model at the given pose
photon::VisionTargetSim visionTarget{targetPose, targetModel};

// Add this vision target to the vision system simulation to make it visible
visionSim.AddVisionTargets({visionTarget});
```

:::{note}
The pose of a `VisionTargetSim` object can be updated with `SetPose()` to simulate moving targets. Note, however, that this will break latency simulation for that target.
:::

If you would rather pull in a whole tag layout, `AddAprilTags()` accepts a WPILib field layout and adds every tag in it under the `"apriltag"` type:

```cpp
// The layout of AprilTags which we want to add to the vision system
wpi::fields::Field tagLayout = wpi::fields::GetField(wpi::fields::FieldId::DEFAULT_FIELD);

visionSim.AddAprilTags(tagLayout);
```

:::{note}
The poses of the AprilTags from this layout depend on its current alliance origin (e.g. blue or red). If this origin is changed later, the targets will have to be cleared from the `VisionSystemSim` and re-added.
:::

## Camera Simulation

Now that we have a simulation world with vision targets, we can add simulated cameras to view it.

Before adding a simulated camera, we need to define its properties. This is done with the `photon::SimCameraProperties` class:

```cpp
// The simulated camera properties
photon::SimCameraProperties cameraProp;
```

By default, this will create a 960 x 720 resolution camera with a 90 degree diagonal FOV(field-of-view) and no noise, distortion, or latency. If we want to change these properties, we can do so:

```cpp
// A 640 x 480 camera with a 100 degree diagonal FOV.
cameraProp.SetCalibration(640, 480, 100_deg);
// Approximate detection noise with average and standard deviation error in pixels.
cameraProp.SetCalibError(0.25, 0.08);
// Set the camera image capture framerate (Note: this is limited by robot loop rate).
cameraProp.SetFPS(20_Hz);
// The average and standard deviation in milliseconds of image data latency.
cameraProp.SetAvgLatency(35_ms);
cameraProp.SetLatencyStdDev(5_ms);
```

PhotonLib also ships presets for common cameras and resolutions, which you can use as a starting point:

```cpp
// A simulated Raspberry Pi 4 running a Lifecam at 640x480
cameraProp = photon::SimCameraProperties::PI4_LIFECAM_640_480();
// Or an OV9281 at 1280x720, as found on many coprocessors
cameraProp = photon::SimCameraProperties::OV9281_1280_720();
```

These properties are used in a `photon::PhotonCameraSim`, which handles generating captured frames of the field from the simulated camera's perspective, and calculating the target data which is sent to the `PhotonCamera` being simulated.

```cpp
// The PhotonCamera used in the real robot code.
photon::PhotonCamera camera{"cameraName"};

// The simulation of this camera. Its values used in real robot code will be updated.
photon::PhotonCameraSim cameraSim{&camera, cameraProp};
```

The `PhotonCameraSim` can now be added to the `VisionSystemSim`. We have to define a robot-to-camera transform, which describes where the camera is relative to the robot pose (this can be measured in CAD or by hand).

```cpp
// Our camera is mounted 0.1 meters forward and 0.5 meters up from the robot pose,
// (Robot pose is considered the center of rotation at the floor level, or Z = 0)
wpi::math::Translation3d robotToCameraTrl{0.1_m, 0_m, 0.5_m};
// and pitched 15 degrees up.
wpi::math::Rotation3d robotToCameraRot{0_rad, -15_deg, 0_rad};
wpi::math::Transform3d robotToCamera{robotToCameraTrl, robotToCameraRot};

// Add this camera to the vision system simulation with the given robot-to-camera transform.
visionSim.AddCamera(&cameraSim, robotToCamera);
```

:::{important}
You may add multiple cameras to one `VisionSystemSim`, but not one camera to multiple `VisionSystemSim`. All targets in the `VisionSystemSim` will be visible to all its cameras.
:::

If the camera is mounted on a mobile mechanism (like a turret) this transform can be updated in a periodic loop.

```cpp
// The turret the camera is mounted on is rotated 5 degrees
wpi::math::Rotation3d turretRotation{0_rad, 0_rad, 5_deg};
robotToCamera = wpi::math::Transform3d{robotToCameraTrl.RotateBy(turretRotation),
                                      robotToCameraRot.RotateBy(turretRotation)};
visionSim.AdjustCamera(&cameraSim, robotToCamera);
```

## Low-Resource Vision Simulation with Photonvision

By default, PhotonCameraSim renders two simulated camera streams using OpenCV:

- Raw stream - The unprocessed camera view
- Processed stream - The camera view with vision processing overlays

These streams are nice if you want to actually view the simulated images, but they can be computationally expensive. This may cause lag and reduced simulation performance on lower-powered computers.

The following configuration disables both streams while still allowing tag detection and pose simulation to work. It's not perfect, but it's much better performance-wise than the default configuration.

```cpp
cameraSim.EnableRawStream(false);        // disables raw image stream
cameraSim.EnabledProcessedStream(false); // disables processed image stream
```

**Use Case**

This configuration is ideal for Chromebooks or low-spec machines where rendering the simulated camera images causes lag, but vision data is still desired for testing.

**What Still Works**

- AprilTag detection
- Pose estimation
- NetworkTables data publishing
- Robot positioning and targeting

**What's Disabled**

- Visual camera stream rendering
- Real-time visual debugging of camera output

## Updating The Simulation World

To update the `VisionSystemSim`, we simply have to pass in the simulated robot pose periodically (in `SimulationPeriodic()`).

```cpp
// Update with the simulated drivetrain pose. This should be called every loop in simulation.
visionSim.Update(robotPose);
```

Targets and cameras can be added and removed, and camera properties can be changed at any time. The `PhotonCameraSim` keeps its properties in a public `prop` member, so they can still be changed after the camera was constructed:

```cpp
cameraSim.prop.SetCalibration(1280, 720, 70_deg);
```

## Visualizing Results

Each `VisionSystemSim` has its own built-in `Field2d` for displaying object poses in the simulation world such as the robot, simulated cameras, and actual/measured target poses.

```cpp
// Get the built-in Field2d used by this VisionSystemSim
visionSim.GetDebugField();
```

```{figure} images/SimExampleField.png
_A_ `VisionSystemSim`_'s internal_ `Field2d` _customized with target images and colors_
```

A `PhotonCameraSim` can also draw and publish generated camera frames to a MJPEG stream similar to an actual PhotonVision process.

```cpp
// Enable the raw and processed streams. These are enabled by default.
cameraSim.EnableRawStream(true);
cameraSim.EnabledProcessedStream(true);

// Enable drawing a wireframe visualization of the field to the camera streams.
// This is extremely resource-intensive and is disabled by default.
cameraSim.EnableDrawWireframe(true);
```

These streams follow the port order mentioned in {ref}`docs/quick-start/networking:Camera Stream Ports`. For example, a single simulated camera will have its raw stream at `localhost:1181` and processed stream at `localhost:1182`, which can also be found in the CameraServer tab of Shuffleboard like a normal camera stream.

```{figure} images/SimExampleFrame.png
_A frame from the processed stream of a simulated camera viewing some 2023 AprilTags with the field wireframe enabled_
```
