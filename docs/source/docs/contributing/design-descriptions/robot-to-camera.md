# Robot to Camera

## How 3D pose estimation works

At its core, Photonvision's 3D pose estimation is built around solving the Perspective-n-Point (PnP) problem. The PnP problem is essentially, 'given a set of points in 3D space, and their projections on a 2D image, determine the pose of the camera'. In photonvision's case, the points are the corners of one or more apriltags.
However, this leaves us with the camera's pose, *not* the robot's pose. The solution, of course, is to apply an offset. For 3D Pose estimation, Photonvision associates to each camera a `Transform3d` object, representing a vector encoding the 6DOF transformation from the robot (or, rather, the point on the robot considered to be its center) to the camera, and the final step of pose estimation is to transform the camera's pose (that got spit out of the PnP solver) by the inverse of this vector, yielding an estimate of the robot's pose. 

## What does the plumbing look like?

The `PhotonCamera` object is (optionally) passed a `Transform3d` object by the Robot code, corresponding to the robot-to-camera transformation. This `Transform3d` (if passed) is transmitted to the coprocessor by the `PhotonCamera`. The coprocessor then appends that `Transform3d` to every result it sends back to the robot controller, where the `PhotonPoseEstimator` object then applies the transformation to the camera pose (If no `Transform3d` is passed to `PhotonCamera`, then no `Transform3d` is appended to the results, and `PhotonPoseEstimator` simply won't work. The result stores the `Transform3d` as an optional)

## That seems complicated and silly. Why not just keep the robot to camera transform in PhotonLib?

We used to! However, a new algorithm for fusing gyroscopic data to PnP pose observations has been added to PhotonVision, Constrained PnP, offering significantly improved accuracy and stability. Notably, this algorithm requires the robot-to-camera transform to run properly. In previous seasons, this wasn't a problem, as Constrained PnP ran entirely on the RoboRIO anyways, in order to access robot gyro data. However, Constrained PnP is very computationally expensive, and so work is being done to offload the work to the coprocessor, and expose gyro data to the coprocessor. The switch to having the robot-to-camera transform exposed to the coprocessor over NetworkTables and becoming an integrated part of Photonvision results is to facilitate the offloading of the Constrained PnP workload (and other estimation workloads that require Gyroscopic data) to the coprocessor
