Installation & Setup
====================

This page will help you install PhotonVision on your coprocessor, wire it, and properly setup the networking in order to start tracking targets.


Step 1: Software Install
------------------------

This section will walk you through how to install PhotonVision on your coprocessor. Your coprocessor is the device that has the camera and you are using to detect targets (ex. if you are using a Limelight / Raspberry Pi, that is your coprocessor and you should follow those instructions).

.. warning:: You only need to install PhotonVision on the coprocessor/device that is being used to detect targets, you do NOT need to install it on the device you use to view the webdashboard. All you need to view the webdashboard is for a device to be on the same network as your vision coprocessor and an internet browser.

.. toctree::
   :maxdepth: 3

   sw_install/index
   updating


Step 2: Wiring
--------------

This section will walk you through how to wire your coprocessor to get power.

.. toctree::
   :maxdepth: 1

   wiring


Step 3: Networking
------------------

This section will walk you though how to connect your coprocessor to a network. This section is very important (and easy to get wrong), so we recommend you read it thoroughly.

.. toctree::
   :maxdepth: 1

   networking
