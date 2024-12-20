# Quick Install

## Install the latest image of photonvision for your coprocessor

- For the supported coprocessors
  - RPI 3,4,5
  - Orange Pi 5
  - Limelight

For installing on non-supported devices {ref}`see. <docs/advanced-installation/sw_install/index:Software Installation>`

[Download the latest preconfigured image of photonvision for your coprocessor](https://github.com/PhotonVision/photonvision/releases/latest)

| Coprocessor          | Image filename                                       | Jar                                   |
| -------------------- | ---------------------------------------------------- | ------------------------------------- |
| OrangePi 5           | photonvision-{version}-linuxarm64_orangepi5.img.xz   | photonvision-{version}-linuxarm64.jar |
| Raspberry Pi 3, 4, 5 | photonvision-{version}-linuxarm64_RaspberryPi.img.xz | photonvision-{version}-linuxarm64.jar |
| Limelight 2          | photonvision-{version}-linuxarm64_limelight2.img.xz  | photonvision-{version}-linuxarm64.jar |
| Limelight 3          | photonvision-{version}-linuxarm64_limelight3.img.xz  | photonvision-{version}-linuxarm64.jar |

:::{warning}
Balena Etcher 1.18.11 is a known working version. Other versions may cause issues such as bootlooping (the system will repeatedly boot and restart) when imaging your device.
:::

Use the 1.18.11 version of [Balena Etcher](https://github.com/balena-io/etcher/releases/tag/v1.18.11) to flash the image onto the coprocessors micro sd card. Select the downloaded `.img.xz` file, select your microSD card, and flash.

Limelights have a different installation processes. Simply connect the limelight to your computer using the proper usb cable. Select the compute module. If it doesn’t show up after 30s try using another USB port, initialization may take a while. If prompted, install the recommended missing drivers. Select the image, and flash.

Unless otherwise noted in release notes or if updating from the prior years version, to update PhotonVision after the initial installation, use the offline update option in the settings page with the downloaded jar file from the latest release.

:::{note}
Limelight 2, 2+, and 3 will need a [custom hardware config file](https://github.com/PhotonVision/photonvision/tree/main/docs/source/docs/advanced-installation/sw_install/files) for lighting to work. Currently only limelight 2 and 2+ files are available.
:::

:::{note}
Raspberry Pi installations may also use the [Raspberry Pi Imager](https://www.raspberrypi.com/software/) to flash the image.

:::
