---
orphan: true
---

# About Pipelines

## What is a pipeline?

A vision pipeline represents a series of steps that are used to acquire an image, process it, and analyzing it to find a target. In most FRC games, this means processing an image in order to detect a piece of retroreflective tape or an AprilTag.

## Types of Pipelines

### AprilTag / ArUco

This pipeline type is based on detecting AprilTag fiducial markers. More information about AprilTags can be found in the [WPILib documentation](https://docs.wpilib.org/en/stable/docs/software/vision-processing/apriltag/apriltag-intro.html). This pipeline provides easy to use 3D pose information which allows localization.

:::{note}
In order to get 3D Pose data about AprilTags, you are required to {ref}`calibrate your camera<docs/calibration/calibration:Calibrating Your Camera>`.
:::

### Object Detection

This pipeline type is based on detecting objects using a neural network. The object detection pipeline uses a pre-trained model to detect objects in the camera stream.

:::{note}
This pipeline type is only supported on the Orange Pi 5/5+ coprocessors due to its Neural Processing Unit used by PhotonVision to support running ML-based object detection.
:::

### Driver Mode

Driver Mode is a type of pipeline that doesn't run any vision processing, intended for human viewing. For more information about Driver Mode, see the {ref}`Driver Mode documentation<docs/driver-mode/index:Driver Mode>`.

### Colored Shape

This pipeline type is based on detecting different shapes like circles, triangles, quadrilaterals, or a polygon. An example usage would be detecting yellow PowerCells from the 2020 FRC game. You can read more about the specific settings available in the contours page.

### Reflective

This pipeline type is based on detecting targets with reflective tape. In the contours tab of this pipeline type, you can filter the area, width/height ratio, fullness, degree of speckle rejection.

:::{note}
This pipeline type is not used anymore due to FRC's removal of retro-reflective tape from the game. It is still available as a pipeline for legacy purposes.
:::

## Note About Multiple Cameras and Pipelines

When using more than one camera, it is important to keep in mind that all cameras run one pipeline each, all publish to NT, and all send both streams. This will have a noticeable affect on performance and we recommend users limit themselves to 1-2 cameras per coprocessor.

## Pipeline Configuration

Each pipeline has a set of tabs that are used to configure the pipeline. All pipelines follow a similar structure with an Input and Output tab, as well as a set of tabs that are specific to the pipeline type.

- Input: This tab allows the raw camera image to be modified before it gets processed. Here, you can set exposure, brightness, gain, orientation, and resolution.

- Output: This allows you to manipulate the detected target via the target offset point (for calculating pitch/yaw) and robot (crosshair) offset. In addition, it allows users to send additional (up to 5) outputs through PhotonLib.

Pipielines also have additional tabs that are specific to the pipeline type. Listed below are the tabs for each pipeline type.

### AprilTag / ArUco Pipelines

- AprilTag: This tab includes AprilTag specific tuning parameters, such as decimate, blur, threads, pose iterations, and more.

### Object Detection Pipelines

- Object Detection: This tab allows you to filter results from the neural network, such as confidence, area, and width/height ratio. The end goal of this tab is to filter out any false positives.

### Reflective and Colored Shape Pipelines

- Threshold: This tab allows you to filter out specific colors/pixels in your camera stream through HSV tuning. The end goal here is having a black and white image that will only have your target lit up.
- Contours: After thresholding, contiguous white pixels are grouped together, and described by a curve that outlines the group. This curve is called a "contour" which represent various targets on your screen. Regardless of type, you can filter how the targets are grouped, their intersection, and how the targets are sorted. Other available filters will change based on different pipeline types.
