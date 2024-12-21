# Best Practices For Competition

## Before Competition

- Ensure you have spares of the relevant electronics if you can afford it (switch, coprocessor, cameras, etc.).
- Stay on the latest version of PhotonVision until you have tested your full robot system to be functional.
- Some time before the competition, lock down the version you are using and do not upgrade unless you encounter a critical bug.
- Have a copy of the installation image for the version you are using on your programming laptop, in case re-imaging (without internet) is needed.
- Extensively test at your home setup. Practice tuning from scratch under different lighting conditions.
- Use SmartDashboard / Shuffleboard to view your camera streams during practice.
- Confirm you have followed all the recommendations under the Networking section in installation (network switch and static IP).
- Only use high quality ethernet cables that have been rigorously tested.
- Set up RIO USB port forwarding using the guide in the Networking section in installation.

## During the Competition

- Use the field calibration time given at the start of the event:
  - Bring your robot to the field at the allotted time.
  - Make sure the field has match-accurate lighting conditions active.
  - Turn on your robot and pull up the dashboard on your driver station.
  - Point your robot at the targets and ensure you get a consistent tracking (you hold one targets consistently, the ceiling lights aren't detected, etc.).
  - If you have problems with your pipeline, go to the pipeline tuning section and retune the pipeline using the guide there.
  - Move the robot close, far, angled, and around the field to ensure no extra targets are found.
  - Monitor camera feeds during a practice match to ensure everything is working correctly.
- After field calibration, use the "Export Settings" button in the "Settings" page to create a backup.
  - Do this for each coprocessor on your robot that runs PhotonVision, and name your exports with meaningful names.
  - This will contain camera information/calibration, pipeline information, network settings, etc.
  - In the event of software/hardware failures (IE lost SD Card, broken device), you can then use the "Import Settings" button and select "All Settings" to restore your settings.
  - This effectively works as a snapshot of your PhotonVision data that can be restored at any point.
- Before every match:
  - Check the ethernet and USB connectors are seated fully.
  - Close streaming dashboards when you don't need them to reduce bandwidth.
- Stream at as low of a resolution as possible while still detecting AprilTags to stay within field bandwidth limits.
