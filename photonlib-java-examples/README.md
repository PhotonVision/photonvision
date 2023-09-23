## PhotonLib Java Examples

### Building

Build photonvision and publish it locally with:

```
photonvision$ ./gradlew photon-lib:publishtomavenlocal
```

Now, cd into here, pull in the latest vendor json, and simulate the project of choice

```
photonvision/photonlib-java-exaples: ./gradlew copyPhotonlib
photonvision/photonlib-java-exaples: ./gradlew aimandrange:simulateJava
```

### [**`aimattarget`**](aimattarget)

A simple demonstration of using PhotonVision's 2d target yaw to align a differential drivetrain with a target.

---

### [**`getinrange`**](getinrange)

A simple demonstration of using PhotonVision's 2d target pitch to bring a differential drivetrain to a specific distance from a target.

---

### [**`aimandrange`**](aimandrange)

A combination of the previous `aimattarget` and `getinrange` examples to simultaneously aim and get in range of a target.

---

### [**`simaimandrange`**](simaimandrange)

The above `aimandrange` example with simulation support.

<img src="https://github-production-user-asset-6210df.s3.amazonaws.com/7953350/268856085-432a54b9-f596-4e30-8b57-a8f38f88f985.png" width=60% height=60%>

**Keyboard controls:**
- Drive forward/backward: W/S
- Turn left/right: A/D
- Perform vision alignment: Z

---

### [**`swervedriveposeestsim`**](swervedriveposeestsim)

A minimal swerve drive example demonstrating the usage of PhotonVision for AprilTag vision estimation with a swerve drive pose estimator.

The example also has simulation support with an approximation of swerve drive dynamics.

<img src="https://github-production-user-asset-6210df.s3.amazonaws.com/7953350/268862944-3392e69a-7705-4dbc-9eb8-0d03a6e27b9e.png" width=60% height=60%>

<img src="https://github-production-user-asset-6210df.s3.amazonaws.com/7953350/268857280-bae145b8-356e-4afb-b842-597dbea60df6.png" width=60% height=60%>

**Keyboard controls:**
- Translate field-relative: WASD
- Rotate counter/clockwise: Q/E
- Offset pose estimate: X
