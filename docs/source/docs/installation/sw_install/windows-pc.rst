Windows PC Installation
=======================
PhotonVision may be run on a Windows Desktop PC for basic testing and evaluation.

.. note:: You do not need to install PhotonVision on a Windows PC in order to access the webdashboard (assuming you are using an external coprocessor like a Raspberry Pi).

Install Bonjour
---------------
Bonjour provides more stable networking when using Windows PCs. Install `Bonjour here <https://support.apple.com/downloads/DL999/en_US/BonjourPSSetup.exe>`_ before continuing to ensure a stable experience while using PhotonVision.

Installing Java
---------------
PhotonVision requires a JDK installed and on the system path. **JDK 11 is needed** (different versions will not work). You may already have this if you have installed WPILib, but ensure that running ``java -version`` shows JDK 11. If not, `download and install it from here <https://adoptium.net/temurin/releases?version=11>`_ and ensure that the new JDK is being used.

.. warning:: Using a JDK other than JDK11 will cause issues when running PhotonVision and is not supported.

Downloading the Latest Stable Release of PhotonVision
-----------------------------------------------------
Go to the `GitHub releases page <https://github.com/PhotonVision/photonvision/releases>`_ and download the winx64.jar file.

Running PhotonVision
--------------------
To run PhotonVision, open a terminal window of your choice and run the following command:

.. code-block::

   > java -jar C:\path\to\photonvision\NAME OF JAR FILE GOES HERE.jar

If your computer has a compatible webcam connected, PhotonVision should startup without any error messages. If there are error messages, your webcam isn't supported or another issue has occurred. If it is the latter, please open an issue on the `PhotonVision issues page <https://github.com/PhotonVision/photonvision/issues>`_.

.. warning:: Using an integrated laptop camera may cause issues when trying to run PhotonVision. If you are unable to run PhotonVision on a laptop with an integrated camera, try disabling the camera's driver in Windows Device Manager.

Accessing the PhotonVision Interface
------------------------------------
Once the Java backend is up and running, you can access the main vision interface by navigating to ``localhost:5800`` inside your browser.
