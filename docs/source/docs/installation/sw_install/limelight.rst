Limelight Installation
======================

Imaging
-------
Limelight imaging is a very similar process to Gloworm, but with extra steps.


Base Install Steps
^^^^^^^^^^^^^^^^^^
Due to the similarities in hardware, follow the :ref:`Gloworm install instructions <docs/installation/sw_install/gloworm:Gloworm Installation>`.


Hardware-Specific Steps
-----------------------
Download the hardwareConfig.json file for the version of your Limelight:

- :download:`Limelight Version 2 <files/Limelight2/hardwareConfig.json>`.
- :download:`Limelight Version 2+ <files/Limelight2+/hardwareConfig.json>`.

.. note:: No hardware config is provided for the Limelight 3 as AprilTags do not require the LEDs (meaning nobody has reverse-engineered what I/O pins drive the LEDs) and the camera FOV is determined as part of calibration.

:ref:`Import the hardwareConfig.json file <docs/additional-resources/config:Importing and Exporting Settings>`. Again, this is **REQUIRED** or target measurements will be incorrect, and LEDs will not work.

After installation you should be able to `locate the camera <https://photonvision.github.io/gloworm-docs/docs/quickstart/#finding-gloworm>`_ at: ``http://photonvision.local:5800/`` (not ``gloworm.local``, as previously)
