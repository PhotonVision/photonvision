# Linux PC Installation

PhotonVision may be run on a Debian-based Linux Desktop PC for basic testing and evaluation.

:::{note}
You do not need to install PhotonVision on a Windows PC in order to access the webdashboard (assuming you are using an external coprocessor like a Raspberry Pi).
:::

## Installing Java

PhotonVision requires a JDK installed and on the system path. JDK 17 is needed (different versions will not work). If you don't have JDK 17 already, run the following to install it:

```
$ sudo apt-get install openjdk-17-jdk
```

:::{warning}
Using a JDK other than JDK17 will cause issues when running PhotonVision and is not supported.
:::

## Downloading the Latest Stable Release of PhotonVision

Go to the [GitHub releases page](https://github.com/PhotonVision/photonvision/releases) and download the relevant .jar file for your coprocessor.

:::{note}
If your coprocessor has a 64 bit ARM based CPU architecture (OrangePi, Raspberry Pi, etc.), download the LinuxArm64.jar file.

If your coprocessor has an 64 bit x86 based CPU architecture (Mini PC, laptop, etc.), download the Linuxx64.jar file.
:::

:::{warning}
Be careful to pick the latest stable release. "Draft" or "Pre-Release" versions are not stable and often have bugs.
:::

## Running PhotonVision

To run PhotonVision, open a terminal window of your choice and run the following command:

```
$ java -jar /path/to/photonvision/photonvision-xxx.jar
```

If your computer has a compatible webcam connected, PhotonVision should startup without any error messages. If there are error messages, your webcam isn't supported or another issue has occurred. If it is the latter, please open an issue on the [PhotonVision issues page](https://github.com/PhotonVision/photonvision/issues).

## Accessing the PhotonVision Interface

Once the Java backend is up and running, you can access the main vision interface by navigating to `localhost:5800` inside your browser.
