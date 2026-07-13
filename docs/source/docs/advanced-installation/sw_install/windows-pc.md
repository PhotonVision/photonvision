# Windows PC Installation

PhotonVision may be run on a Windows Desktop PC for basic testing and evaluation.

:::{note}
You do not need to install PhotonVision on a Windows PC in order to access the webdashboard (assuming you are using an external coprocessor like a Raspberry Pi).
:::

## Install Bonjour

Bonjour provides more stable networking when using Windows PCs. Install [Bonjour here](https://support.apple.com/downloads/DL999/en_US/BonjourPSSetup.exe) before continuing to ensure a stable experience while using PhotonVision.

(java-on-windows)=
## Installing Java

PhotonVision requires a JDK installed and on the system path. **Windows Users must use the JDK that ships with WPILib.** After installing WPILib, the JDK will be located in the directory where WPILib is installed. Typically this is `C:\Users\Public\wpilib\YYYY\jdk\bin` wheere YYYY is the year. Copy the full path to the JDK `\bin` directory and add it to the beginning of your PATH.

## Downloading the Latest Stable Release of PhotonVision

Go to the [GitHub releases page](https://github.com/PhotonVision/photonvision/releases) and download the winx86-64.jar file.

## Running PhotonVision

To run PhotonVision, open a terminal window of your choice and run the following command:

```
> java -jar C:\path\to\photonvision\photonvision-XXX.jar
```

If your computer has a compatible webcam connected, PhotonVision should startup without any error messages. If there are error messages, your webcam isn't supported or another issue has occurred. If it is the latter, please open an issue on the [PhotonVision issues page](https://github.com/PhotonVision/photonvision/issues).

:::{warning}
Using an integrated laptop camera may cause issues when trying to run PhotonVision. If you are unable to run PhotonVision on a laptop with an integrated camera, try disabling the camera's driver in Windows Device Manager.
:::

## Accessing the PhotonVision Interface

Once the Java backend is up and running, you can access the main vision interface by navigating to `localhost:5800` inside your browser.
