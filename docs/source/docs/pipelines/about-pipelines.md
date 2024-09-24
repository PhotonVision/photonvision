---
orphan: true
---

# About Pipelines

## What is a pipeline?

A vision pipeline represents a series of steps that are used to acquire an image, process it, and analyzing it to find a target. In most FRC games, this means processing an image in order to detect a piece of retroreflective tape or an AprilTag.

## Types of Pipelines

### Reflective

This is the most common pipeline type and it is based on detecting targets with retroreflective tape. In the contours tab of this pipeline type, you can filter the area, width/height ratio, fullness, degree of speckle rejection.

### Colored Shape

This pipeline type is based on detecting different shapes like circles, triangles, quadrilaterals, or a polygon. An example usage would be detecting yellow PowerCells from the 2020 FRC game. You can read more about the specific settings available in the contours page.

### AprilTag / AruCo

This pipeline type is based on detecting AprilTag fiducial markers. More information about AprilTags can be found in the WPILib documentation. While being more performance intensive than the reflective and colored shape pipeline, it has the benefit of providing easy to use 3D pose information which allows localization.

:::{note}
In order to get 3D Pose data about AprilTags, you are required to {ref}`calibrate your camera<docs/calibration/calibration:Calibrating Your Camera>`.
:::

## Note About Multiple Cameras and Pipelines

When using more than one camera, it is important to keep in mind that all cameras run one pipeline each, all publish to NT, and all send both streams. This will have a noticeable affect on performance and we recommend users limit themselves to 1-2 cameras per coprocessor.

## Pipeline Steps

Reflective and Colored Shape Pipelines have 4 steps (represented as 4 tabs):

1. Input: This tab allows the raw camera image to be modified before it gets processed. Here, you can set exposure, brightness, gain, orientation, and resolution.
2. Threshold (Only Reflective and Colored Shape): This tabs allows you to filter our specific colors/pixels in your camera stream through HSV tuning. The end goal here is having a black and white image that will only have your target lit up.
3. Contours: After thresholding, contiguous white pixels are grouped together, and described by a curve that outlines the group. This curve is called a "contour" which represent various targets on your screen. Regardless of type, you can filter how the targets are grouped, their intersection, and how the targets are sorted. Other available filters will change based on different pipeline types.
4. Output: Now that you have filtered all of your contours, this allows you to manipulate the detected target via orientation, the offset point, and offset.

AprilTag / AruCo Pipelines have 3 steps:

1. Input: This is the same as the above.
2. AprilTag: This step include AprilTag specific tuning parameters, such as decimate, blur, threads, pose iterations, and more.
3. Output: This is the same as the above.
