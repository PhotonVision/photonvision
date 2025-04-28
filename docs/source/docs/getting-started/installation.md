# Installation

## Install the latest image of photonvision for your coprocessor

- For the supported coprocessors
  - RPI 3,4,5
  - Orange Pi 5
  - Limelight

[Download the latest preconfigured image of photonvision for your coprocessor](https://github.com/PhotonVision/photonvision/releases/latest)

| Coprocessor          | Image filename                                       | Jar                                   |
| -------------------- | ---------------------------------------------------- | ------------------------------------- |
| OrangePi 5           | photonvision-{version}-linuxarm64_orangepi5.img.xz   | photonvision-{version}-linuxarm64.jar |
| Raspberry Pi 3, 4, 5 | photonvision-{version}-linuxarm64_RaspberryPi.img.xz | photonvision-{version}-linuxarm64.jar |
| Limelight 2          | photonvision-{version}-linuxarm64_limelight2.img.xz  | photonvision-{version}-linuxarm64.jar |
| Limelight 3          | photonvision-{version}-linuxarm64_limelight3.img.xz  | photonvision-{version}-linuxarm64.jar |

Use the [Raspberry Pi Imager](https://www.raspberrypi.com/software/) to flash the image onto the coprocessors microSD card. Select the downloaded `.img.xz` file, select your microSD card, and flash.

:::{warning}
Balena Etcher can also be used, but historically has had issues such as bootlooping (the system will repeatedly boot and restart) when imaging your device. Use at your own risk.
:::

Limelights have a different installation processes. Simply connect the limelight to your computer using the proper usb cable. Select the compute module. If it doesn’t show up after 30s try using another USB port, initialization may take a while. If prompted, install the recommended missing drivers. Select the image, and flash.

Unless otherwise noted in release notes or if updating from the prior years version, to update PhotonVision after the initial installation, use the offline update option in the settings page with the downloaded jar file from the latest release.

:::{note}
Limelight 2, 2+, and 3 will need a [custom hardware config file](https://github.com/PhotonVision/photonvision/tree/main/docs/source/docs/advanced-installation/sw_install/files) for lighting to work. Currently only limelight 2 and 2+ files are available.
:::

For installation on other coprocessors, refer to the {ref}`docs/advanced-installation/index:Advanced Installation` page.
