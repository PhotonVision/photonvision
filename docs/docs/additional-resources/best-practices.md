# Best Practices For Competition

## Before Competition

- Ensure you have spares of the relevant electronics if you can afford it (switch, coprocessor, cameras, etc.).
- Download the latest release .jar onto your computer and update your Pi if necessary (only update if the release is labeled "critical" or similar, we do not recommend updating right before an event in case there are unforeseen bugs).
- Test out PhotonVision at your home setup.
- Ensure that you have set up SmartDashboard / Shuffleboard to view your camera streams during matches.
- Follow all the recommendations under the Networking section in installation (network switch and static IP).
- Use high quality ethernet cables that have been rigorously tested.
- Set up port forwarding using the guide in the Networking section in installation.

## During the Competition

- Make sure you take advantage of the field calibration time given at the start of the event:
    - Bring your robot to the field at the allotted time.
    - Turn on your robot and pull up the dashboard on your driver station.
    - Point your robot at the target(s) and ensure you get a consistent tracking (you hold one target consistently, the ceiling lights aren't detected, etc.).
    - If you have problems with your pipeline, go to the pipeline tuning section and retune the pipeline using the guide there. You want to make your exposure as low as possible with a tight hue value to ensure no extra targets are detected.
    - Move the robot close, far, angled, and around the field to ensure no extra targets are found anywhere when looking for a target.
    - Go to a practice match to ensure everything is working correctly.
- After field calibration, use the "Export Settings" button in the "Settings" page to create a backup.
    - Do this for each coprocessor on your robot that runs PhotonVision, and name your exports with meaningful names.
    - This will contain camera information/calibration, pipeline information, network settings, etc.
    - In the event of software/hardware failures (IE lost SD Card, broken device), you can then use the "Import Settings" button and select "All Settings" to restore your settings.
    - This effectively works as a snapshot of your PhotonVision data that can be restored at any point.
- Before every match, check the ethernet connection going into your coprocessor and that it is seated fully.
- Ensure that exposure is as low as possible and that you don't have the dashboard up when you don't need it to reduce bandwidth.
- Stream at as low of a resolution as possible while still detecting targets to stay within bandwidth limits.
