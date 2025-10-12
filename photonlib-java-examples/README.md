## PhotonLib Java Examples

All examples demonstrate controlling a swerve drive with outputs from PhotonVision.

Simulation is available to demonstrate the concepts - swerve physics is approximated.

You can access a stream of what the simulated camera sees by going to https://localhost:1182 .

### Running examples

For instructions on how to run these examples locally, see [Running Examples](https://docs.photonvision.org/en/latest/docs/contributing/building-photon.html#running-examples).

---

### [**`aimattarget`**](aimattarget)

A simple demonstration of using PhotonVision's 2d target yaw to align a differential drivetrain with a target.

**Keyboard controls:**
- Translate field-relative: WASD
- Rotate counter/clockwise: Q/E
- Perform vision alignment: Z

---

### [**`aimandrange`**](aimandrange)

Extends`aimattarget` to add getting in range of the target.

**Keyboard controls:**
- Translate field-relative: WASD
- Rotate counter/clockwise: Q/E
- Perform vision alignment: Z

---

### [**`poseest`**](poseest)


The example also has simulation support with an approximation of swerve drive dynamics.

<img src="https://github-production-user-asset-6210df.s3.amazonaws.com/7953350/268862944-3392e69a-7705-4dbc-9eb8-0d03a6e27b9e.png" width=60% height=60%>

<img src="https://github-production-user-asset-6210df.s3.amazonaws.com/7953350/268857280-bae145b8-356e-4afb-b842-597dbea60df6.png" width=60% height=60%>

**Keyboard controls:**
- Translate field-relative: WASD
- Rotate counter/clockwise: Q/E
- Perform vision alignment: Z
- Offset pose estimate: X
