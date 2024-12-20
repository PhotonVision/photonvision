# Getting Target Data

## Constructing a PhotonCamera

### What is a PhotonCamera?

`PhotonCamera` is a class in PhotonLib that allows a user to interact with one camera that is connected to hardware that is running PhotonVision. Through this class, users can retrieve yaw, pitch, roll, robot-relative pose, latency, and a wealth of other information.

The `PhotonCamera` class has two constructors: one that takes a `NetworkTable` and another that takes in the name of the network table that PhotonVision is broadcasting information over. For ease of use, it is recommended to use the latter. The name of the NetworkTable (for the string constructor) should be the same as the camera's nickname (from the PhotonVision UI).

```{eval-rst}
.. tab-set-code::


     .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/a3bcd3ac4f88acd4665371abc3073bdbe5effea8/photonlib-java-examples/src/main/java/org/photonlib/examples/aimattarget/Robot.java
        :language: java
        :lines: 51-52

     .. rli:: https://github.com/PhotonVision/photonvision/raw/a3bcd3ac4f88acd4665371abc3073bdbe5effea8/photonlib-cpp-examples/src/main/cpp/examples/aimattarget/include/Robot.h
        :language: c++
        :lines: 42-43

     .. code-block:: Python

         # Change this to match the name of your camera as shown in the web ui
         self.camera = PhotonCamera("your_camera_name_here")

```

:::{warning}
Teams must have unique names for all of their cameras regardless of which coprocessor they are attached to.
:::

## Getting the Pipeline Result

### What is a Photon Pipeline Result?

A `PhotonPipelineResult` is a container that contains all information about currently detected targets from a `PhotonCamera`. You can retrieve the latest pipeline result using the PhotonCamera instance.

Use the `getLatestResult()`/`GetLatestResult()` (Java and C++ respectively) to obtain the latest pipeline result. An advantage of using this method is that it returns a container with information that is guaranteed to be from the same timestamp. This is important if you are using this data for latency compensation or in an estimator.

```{eval-rst}
.. tab-set-code::


     .. rli:: https://raw.githubusercontent.com/PhotonVision/photonvision/a3bcd3ac4f88acd4665371abc3073bdbe5effea8/photonlib-java-examples/src/main/java/org/photonlib/examples/aimattarget/Robot.java
        :language: java
        :lines: 79-80

     .. rli:: https://github.com/PhotonVision/photonvision/raw/a3bcd3ac4f88acd4665371abc3073bdbe5effea8/photonlib-cpp-examples/src/main/cpp/examples/aimattarget/cpp/Robot.cpp
         :language: c++
         :lines: 35-36

     .. code-block:: Python

         # Query the latest result from PhotonVision
         result = self.camera.getLatestResult()


```

:::{note}
Unlike other vision software solutions, using the latest result guarantees that all information is from the same timestamp. This is achievable because the PhotonVision backend sends a byte-packed string of data which is then deserialized by PhotonLib to get target data. For more information, check out the [PhotonLib source code](https://github.com/PhotonVision/photonvision/tree/main/photon-lib).
:::

## Checking for Existence of Targets

Each pipeline result has a `hasTargets()`/`HasTargets()` (Java and C++ respectively) method to inform the user as to whether the result contains any targets.

```{eval-rst}
.. tab-set-code::
   .. code-block:: Java

      // Check if the latest result has any targets.
      boolean hasTargets = result.hasTargets();

   .. code-block:: C++

      // Check if the latest result has any targets.
      bool hasTargets = result.HasTargets();

   .. code-block:: Python

     # Check if the latest result has any targets.
      hasTargets = result.hasTargets()
```

:::{warning}
In Java/C++, You must *always* check if the result has a target via `hasTargets()`/`HasTargets()` before getting targets or else you may get a null pointer exception. Further, you must use the same result in every subsequent call in that loop.
:::

## Getting a List of Targets

### What is a Photon Tracked Target?

A tracked target contains information about each target from a pipeline result. This information includes yaw, pitch, area, and robot relative pose.

You can get a list of tracked targets using the `getTargets()`/`GetTargets()` (Java and C++ respectively) method from a pipeline result.

```{eval-rst}
.. tab-set-code::
   .. code-block:: Java

      // Get a list of currently tracked targets.
      List<PhotonTrackedTarget> targets = result.getTargets();

   .. code-block:: C++

      // Get a list of currently tracked targets.
      wpi::ArrayRef<photonlib::PhotonTrackedTarget> targets = result.GetTargets();

   .. code-block:: Python

      # Get a list of currently tracked targets.
      targets = result.getTargets()
```

## Getting the Best Target

You can get the {ref}`best target <docs/reflectiveAndShape/contour-filtering:Contour Grouping and Sorting>` using `getBestTarget()`/`GetBestTarget()` (Java and C++ respectively) method from the pipeline result.

```{eval-rst}
.. tab-set-code::
   .. code-block:: Java

      // Get the current best target.
      PhotonTrackedTarget target = result.getBestTarget();

   .. code-block:: C++

      // Get the current best target.
      photonlib::PhotonTrackedTarget target = result.GetBestTarget();


   .. code-block:: Python

      # Coming Soon!

```

## Getting Data From A Target

- double `getYaw()`/`GetYaw()`: The yaw of the target in degrees (positive right).
- double `getPitch()`/`GetPitch()`: The pitch of the target in degrees (positive up).
- double `getArea()`/`GetArea()`: The area (how much of the camera feed the bounding box takes up) as a percent (0-100).
- double `getSkew()`/`GetSkew()`: The skew of the target in degrees (counter-clockwise positive).
- double\[\] `getCorners()`/`GetCorners()`: The 4 corners of the minimum bounding box rectangle.
- Transform2d `getCameraToTarget()`/`GetCameraToTarget()`: The camera to target transform. See [2d transform documentation here](https://docs.wpilib.org/en/latest/docs/software/advanced-controls/geometry/transformations.html#transform2d-and-twist2d).

```{eval-rst}
.. tab-set-code::
   .. code-block:: Java

      // Get information from target.
      double yaw = target.getYaw();
      double pitch = target.getPitch();
      double area = target.getArea();
      double skew = target.getSkew();
      Transform2d pose = target.getCameraToTarget();
      List<TargetCorner> corners = target.getCorners();

   .. code-block:: C++

      // Get information from target.
      double yaw = target.GetYaw();
      double pitch = target.GetPitch();
      double area = target.GetArea();
      double skew = target.GetSkew();
      frc::Transform2d pose = target.GetCameraToTarget();
      wpi::SmallVector<std::pair<double, double>, 4> corners = target.GetCorners();

   .. code-block:: Python

      # Get information from target.
      yaw = target.getYaw()
      pitch = target.getPitch()
      area = target.getArea()
      skew = target.getSkew()
      pose = target.getCameraToTarget()
      corners = target.getDetectedCorners()
```

## Getting AprilTag Data From A Target

:::{note}
All of the data above (**except skew**) is available when using AprilTags.
:::

- int `getFiducialId()`/`GetFiducialId()`: The ID of the detected fiducial marker.
- double `getPoseAmbiguity()`/`GetPoseAmbiguity()`: How ambiguous the pose of the target is (see below).
- Transform3d `getBestCameraToTarget()`/`GetBestCameraToTarget()`: Get the transform that maps camera space (X = forward, Y = left, Z = up) to object/fiducial tag space (X forward, Y left, Z up) with the lowest reprojection error.
- Transform3d `getAlternateCameraToTarget()`/`GetAlternateCameraToTarget()`: Get the transform that maps camera space (X = forward, Y = left, Z = up) to object/fiducial tag space (X forward, Y left, Z up) with the highest reprojection error.

```{eval-rst}
.. tab-set-code::
   .. code-block:: Java

      // Get information from target.
      int targetID = target.getFiducialId();
      double poseAmbiguity = target.getPoseAmbiguity();
      Transform3d bestCameraToTarget = target.getBestCameraToTarget();
      Transform3d alternateCameraToTarget = target.getAlternateCameraToTarget();

   .. code-block:: C++

      // Get information from target.
      int targetID = target.GetFiducialId();
      double poseAmbiguity = target.GetPoseAmbiguity();
      frc::Transform3d bestCameraToTarget = target.getBestCameraToTarget();
      frc::Transform3d alternateCameraToTarget = target.getAlternateCameraToTarget();

   .. code-block:: Python

      # Get information from target.
      targetID = target.getFiducialId()
      poseAmbiguity = target.getPoseAmbiguity()
      bestCameraToTarget = target.getBestCameraToTarget()
      alternateCameraToTarget = target.getAlternateCameraToTarget()
```

## Saving Pictures to File

A `PhotonCamera` can save still images from the input or output video streams to file. This is useful for debugging what a camera is seeing while on the field and confirming targets are being identified properly.

Images are stored within the PhotonVision configuration directory. Running the "Export" operation in the settings tab will download a .zip file which contains the image captures.

```{eval-rst}
.. tab-set-code::

    .. code-block:: Java

      // Capture pre-process camera stream image
      camera.takeInputSnapshot();

      // Capture post-process camera stream image
      camera.takeOutputSnapshot();

    .. code-block:: C++

      // Capture pre-process camera stream image
      camera.TakeInputSnapshot();

      // Capture post-process camera stream image
      camera.TakeOutputSnapshot();

    .. code-block:: Python

      # Capture pre-process camera stream image
      camera.takeInputSnapshot()

      # Capture post-process camera stream image
      camera.takeOutputSnapshot()
```

:::{note}
Saving images to file takes a bit of time and uses up disk space, so doing it frequently is not recommended. In general, the camera will save an image every 500ms. Calling these methods faster will not result in additional images. Consider tying image captures to a button press on the driver controller, or an appropriate point in an autonomous routine.
:::
