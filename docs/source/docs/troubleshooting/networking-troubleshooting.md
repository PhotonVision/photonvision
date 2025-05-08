# Networking Troubleshooting

Before reading further, ensure that you follow all the recommendations {ref}`in our networking section <docs/quick-start/networking:Physical Networking>`. You should follow these guidelines in order for PhotonVision to work properly; other networking setups are not officially supported.

## Checklist

A few issues make up the majority of support requests. Run through this checklist quickly to catch some common mistakes.

- Is your camera connected to the robot's radio through a {ref}`network switch <docs/quick-start/networking:Physical Networking>`?
  - Ethernet straight from a laptop to a coprocessor will not work (most likely), due to the unreliability of link-local connections.
  - Even if there's a switch between your laptop and coprocessor, you'll still want a radio or router in the loop somehow.
  - The FRC radio is the _only_ router we will officially support due to the innumerable variations between routers.
- (Raspberry Pi, Orange Pi & Limelight only) have you flashed the correct image, and is it [up to date](https://github.com/PhotonVision/photonvision/releases/latest)?
- Is your robot code using a **2025** version of WPILib, and is your coprocessor using the most up to date **2025** release?
  - 2022, 2023, 2024, and 2025 versions of either cannot be mix-and-matched!
  - Your PhotonVision version can be checked on the settings tab.
- Is your team number correctly set on the settings tab?

### photonvision.local Not Found

Use [Angry IP Scanner](https://angryip.org/) and look for an IP that has port 5800 open. Then go to your web browser and do \<IP ADDRESS>:5800.

Alternatively, you can plug your coprocessor into a display, plug in a keyboard, and run `hostname -I` in the terminal. This should give you the IP Address of your coprocessor, then go to your web browser and do \<IP ADDRESS>:5800.

If nothing shows up, ensure your coprocessor has power, and you are following all of our networking recommendations, feel free to {ref}`contact us <index:contact us>` and we will help you.

### Can't Connect To Robot

Please check that:
1\. You don't have the NetworkTables Server on (toggleable in the settings tab). Turn this off when doing work on a robot.
2\. You have your team number set properly in the settings tab.
3\. Your camera name in the `PhotonCamera` constructor matches the name in the UI.
4\. You are using the 2025 version of WPILib and RoboRIO image.
5\. Your robot is on.

If all of the above are met and you still have issues, feel free to {ref}`contact us <index:contact us>` and provide the following information:

- The WPILib version used by your robot code
- PhotonLib vendor dependency version
- PhotonVision version (from the UI)
- Your settings exported from your coprocessor (if you're able to access it)
- How your RoboRIO/coprocessor are networked together
