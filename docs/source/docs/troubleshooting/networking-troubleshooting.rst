Networking Troubleshooting
==========================

Before reading further, ensure that you follow all the recommendations :ref:`in our networking section <docs/installation/networking:Physical Networking>`. You should follow these guidelines in order for PhotonVision to work properly; other networking setups are not officially supported.


Checklist
^^^^^^^^^

A few issues make up the majority of support requests. Run through this checklist quickly to catch some common mistakes.

- Is your camera connected to the robot's radio through a :ref:`network switch <docs/installation/networking:Physical Networking>`?
   - Ethernet straight from a laptop to a coprocessor will not work (most likely), due to the unreliability of link-local connections.
   - Even if there's a switch between your laptop and coprocessor, you'll still want a radio or router in the loop somehow.
   - The FRC radio is the *only* router we will officially support due to the innumerable variations between routers.
- (Raspberry Pi, Orange Pi & Limelight only) have you flashed the correct image, and is it up to date?
   - Limelights 2/2+ and Gloworms should be flashed using the Limelight 2 image (eg, `photonvision-v2024.2.8-linuxarm64_limelight2.img.xz`).
   - Limelights 3 should be flashed using the Limelight 3 image (eg, `photonvision-v2024.2.8-linuxarm64_limelight3.img.xz`).
   - Raspberry Pi devices (including Pi 3, Pi 4, CM3 and CM4) should be flashed using the Raspberry Pi image (eg, `photonvision-v2024.2.8-linuxarm64_RaspberryPi.img.xz`).
   - Orange Pi 5 devices should be flashed using the Orange Pi 5 image (eg, `photonvision-v2024.2.8-linuxarm64_orangepi5.img.xz`).
   - Orange Pi 5+ devices should be flashed using the Orange Pi 5+ image (eg, `photonvision-v2024.2.8-linuxarm64_orangepi5plus.img.xz`).
- Is your robot code using a **2024** version of WPILib, and is your coprocessor using the most up to date **2024** release?
   - 2022, 2023 and 2024 versions of either cannot be mix-and-matched!
   - Your PhotonVision version can be checked on the :ref:`settings tab<docs/settings:settings>`.
- Is your team number correctly set on the :ref:`settings tab<docs/settings:settings>`?


photonvision.local Not Found
----------------------------

Use `Angry IP Scanner <https://angryip.org/>`_ and look for an IP that has port 5800 open. Then go to your web browser and do <IP ADDRESS>:5800.

Alternatively, you can plug your coprocessor into a display, plug in a keyboard, and run ``hostname -I`` in the terminal. This should give you the IP Address of your coprocessor, then go to your web browser and do <IP ADDRESS>:5800.

If nothing shows up, ensure your coprocessor has power, and you are following all of our networking recommendations, feel free to :ref:`contact us <index:contact us>` and we will help you.

Can't Connect To Robot
----------------------

Please check that:
1. You don't have the NetworkTables Server on (toggleable in the settings tab). Turn this off when doing work on a robot.
2. You have your team number set properly in the settings tab.
3. Your camera name in the ``PhotonCamera`` constructor matches the name in the UI.
4. You are using the 2024 version of WPILib and RoboRIO image.
5. Your robot is on.

If all of the above are met and you still have issues, feel free to :ref:`contact us <index:contact us>` and provide the following information:

- The WPILib version used by your robot code
- PhotonLib vendor dependency version
- PhotonVision version (from the UI)
- Your settings exported from your coprocessor (if you're able to access it)
- How your RoboRIO/coprocessor are networked together
