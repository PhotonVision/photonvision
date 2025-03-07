# Windows PC Installation

PhotonVision may be run on a Windows Desktop PC for basic testing and evaluation.

:::{note}
You do not need to install PhotonVision on a Windows PC in order to access the webdashboard (assuming you are using an external coprocessor like a Raspberry Pi).
:::

## Install Bonjour

Bonjour provides more stable networking when using Windows PCs. Install [Bonjour here](https://support.apple.com/downloads/DL999/en_US/BonjourPSSetup.exe) before continuing to ensure a stable experience while using PhotonVision.

## Installing Java

PhotonVision requires a JDK installed and on the system path. **JDK 17 is needed. Windows Users must use the JDK that ships with WPILib.** [Download and install it from here.](https://github.com/wpilibsuite/allwpilib/releases/tag/v2025.3.1) Either ensure the only Java on your PATH is the WPILIB Java or specify it to gradle with `-Dorg.gradle.java.home=C:\Users\Public\wpilib\2025\jdk`:

```
> ./gradlew run "-Dorg.gradle.java.home=C:\Users\Public\wpilib\2025\jdk"
```

:::{warning}
Using a JDK other than WPILIB's JDK17 will cause issues when running PhotonVision and is not supported.
:::

## Downloading the Latest Stable Release of PhotonVision

Go to the [GitHub releases page](https://github.com/PhotonVision/photonvision/releases) and download the winx64.jar file.

## Running PhotonVision

To run PhotonVision, open a terminal window of your choice and run the following command:

```
> java -jar C:\path\to\photonvision\NAME OF JAR FILE GOES HERE.jar
```

If your computer has a compatible webcam connected, PhotonVision should startup without any error messages. If there are error messages, your webcam isn't supported or another issue has occurred. If it is the latter, please open an issue on the [PhotonVision issues page](https://github.com/PhotonVision/photonvision/issues).

:::{warning}
Using an integrated laptop camera may cause issues when trying to run PhotonVision. If you are unable to run PhotonVision on a laptop with an integrated camera, try disabling the camera's driver in Windows Device Manager.
:::

## Accessing the PhotonVision Interface

Once the Java backend is up and running, you can access the main vision interface by navigating to `localhost:5800` inside your browser.
