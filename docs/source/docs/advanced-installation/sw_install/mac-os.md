# Mac OS Installation

:::{warning}
Due to current [cscore](https://github.com/wpilibsuite/allwpilib/tree/main/cscore) restrictions, the PhotonVision server backend may have issues running macOS.
:::

:::{note}
You do not need to install PhotonVision on a Mac in order to access the webdashboard (assuming you are using an external coprocessor like a Raspberry Pi).
:::

VERY Limited macOS support is available.

## Installing Java

PhotonVision requires a JDK installed and on the system path. JDK 17 is needed (different versions will not work). You may already have this if you have installed WPILib 2025+. If not, [download and install it from here](https://adoptium.net/temurin/releases?version=17).

:::{warning}
Using a JDK other than JDK17 will cause issues when running PhotonVision and is not supported.
:::

## Downloading the Latest Stable Release of PhotonVision

Go to the [GitHub releases page](https://github.com/PhotonVision/photonvision/releases) and download the relevant .jar file for your coprocessor.

:::{note}
If you have an M Series Mac, download the macarm64.jar file.

If you have an Intel based Mac, download the macx64.jar file.
:::

:::{warning}
Be careful to pick the latest stable release. "Draft" or "Pre-Release" versions are not stable and often have bugs.
:::

## Running PhotonVision

To run PhotonVision, open a terminal window of your choice and run the following command:

```
$ java -jar /path/to/photonvision/photonvision-xxx.jar
```

:::{warning}
Due to current [cscore](https://github.com/wpilibsuite/allwpilib/tree/main/cscore) restrictions, the PhotonVision using test mode is all that is known to work currently.
:::

## Accessing the PhotonVision Interface

Once the Java backend is up and running, you can access the main vision interface by navigating to `localhost:5800` inside your browser.

:::{warning}
Due to current [cscore](https://github.com/wpilibsuite/allwpilib/tree/main/cscore) restrictions, it is unlikely any streams will open from real webcams.
:::
