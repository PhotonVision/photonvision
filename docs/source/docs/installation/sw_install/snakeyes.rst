SnakeEyes Installation
======================
A Pre-Built Raspberry Pi image with configuration for `the SnakeEyes Raspberry Pi Hat <https://www.playingwithfusion.com/productview.php?pdid=133&catid=1014>`_ is available for ease of setup.

Downloading the SnakeEyes Image
-------------------------------
Download the latest release of the SnakeEyes-specific PhotonVision Pi image from the `releases page <https://github.com/PlayingWithFusion/SnakeEyesDocs/releases>`_. You do not need to extract the downloaded ZIP file.

Flashing the SnakeEyes Image
----------------------------
An 8GB or larger card is recommended.

Use the 1.18.11 version of `Balena Etcher <https://github.com/balena-io/etcher/releases/tag/v1.18.11>`_ to flash an image onto a Raspberry Pi. Select the downloaded ``.zip`` file, select your microSD card, and flash.

For more detailed instructions on using Etcher, please see the `Etcher website <https://www.balena.io/etcher/>`_.

.. warning:: Using a version of Balena Etcher older than 1.18.11 may cause bootlooping (the system will repeatedly boot and restart) when imaging your Raspberry Pi. Updating to 1.18.11 will fix this issue.

Alternatively, you can use the `Raspberry Pi Imager <https://www.raspberrypi.com/software/>`_ to flash the image.

Select "Choose OS" and then "Use custom" to select the downloaded image file. Select your microSD card and flash.

Final Steps
-----------
Insert the flashed microSD card into your Raspberry Pi and boot it up. The first boot may take a few minutes as the Pi expands the filesystem. Be sure not to unplug during this process.

After the initial setup process, your Raspberry Pi should be configured for PhotonVision. You can verify this by making sure your Raspberry Pi and computer are connected to the same network and navigating to ``http://photonvision.local:5800`` in your browser on your computer.

Troubleshooting/Setting a Static IP
-----------------------------------
A static IP address may be used as an alternative to the mDNS ``photonvision.local`` address.

Download and run `Angry IP Scanner <https://angryip.org/download/#windows>`_ to find PhotonVision/your coprocessor on your network.

.. image:: images/angryIP.png

Once you find it, set the IP to a desired :ref:`static IP in PhotonVision. <docs/settings:Networking>`

Updating PhotonVision
----------------------
Download the latest xxxxx-LinuxArm64.jar from `our releases page <https://github.com/PhotonVision/photonvision/releases>`_, go to the settings tab, and upload the .jar using the Offline Update button.

As an alternative option - Export your settings, reimage your coprocessor using the instructions above, and import your settings back in.

Hardware Troubleshooting
------------------------
To turn the LED lights off or on you need to modify the ``ledMode`` network tables entry or the ``camera.setLED`` of PhotonLib.

Support Links
-------------

* `Website <https://www.playingwithfusion.com/productview.php?pdid=133>`__

* `Image <https://github.com/PlayingWithFusion/SnakeEyesDocs/releases/latest>`__

* `Documentation <https://github.com/PlayingWithFusion/SnakeEyesDocs/blob/master/PhotonVision/readme.md>`__
