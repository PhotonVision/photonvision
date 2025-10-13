# Quick Installation Guide

- For the following supported coprocessors
  - {ref}`Raspberry Pi 3,4,5 <docs/quick-start/quick-install:Raspberry Pi and Orange Pi Installation>`
  - {ref}`Orange Pi 5, 5B, 5 Pro <docs/quick-start/quick-install:Raspberry Pi and Orange Pi Installation>`
  - {ref}`Limelight 2, 2+, 3, 3G, 4 <docs/quick-start/quick-install:LimeLight Installation>`
  - {ref}`Rubik Pi 3 <docs/quick-start/quick-install:Rubik Pi 3 Installation>`

For installing on non-supported devices {ref}`see here. <docs/advanced-installation/sw_install/index:Software Installation>`

[Download the latest preconfigured image of photonvision for your coprocessor](https://github.com/PhotonVision/photonvision/releases/latest)

| Coprocessor          | Image filename                                           | Jar                                   |
| -------------------- | -------------------------------------------------------- | ------------------------------------- |
| Raspberry Pi 3, 4, 5 | photonvision-{version}-linuxarm64_RaspberryPi.img.xz     | photonvision-{version}-linuxarm64.jar |
| OrangePi 5           | photonvision-{version}-linuxarm64_orangepi5.img.xz       | photonvision-{version}-linuxarm64.jar |
| OrangePi 5B          | photonvision-{version}-linuxarm64_orangepi5b.img.xz      | photonvision-{version}-linuxarm64.jar |
| OrangePi 5 Pro       | photonvision-{version}-linuxarm64_orangepi5pro.img.xz    | photonvision-{version}-linuxarm64.jar |
| Limelight 2          | photonvision-{version}-linuxarm64_limelight2.img.xz      | photonvision-{version}-linuxarm64.jar |
| Limelight 3          | photonvision-{version}-linuxarm64_limelight3.img.xz      | photonvision-{version}-linuxarm64.jar |
| Limelight 3G         | photonvision-{version}-linuxarm64_limelight3G.img.xz     | photonvision-{version}-linuxarm64.jar |
| Limelight 4          | photonvision-{version}-linuxarm64_limelight4.img.xz      | photonvision-{version}-linuxarm64.jar |
| Rubik Pi 3           | photonvision-{version}-linuxarm64_rubikpi3.tar.xz        | photonvision-{version}-linuxarm64.jar |

Unless otherwise noted in release notes or if updating from the prior years version, to update PhotonVision after the initial installation, use the offline update option in the settings page with the downloaded jar file from the latest release.

## Raspberry Pi and Orange Pi Installation

Use the [Raspberry Pi Imager](https://www.raspberrypi.com/software/) to flash the image onto the coprocessors microSD card. Select the downloaded `.img.xz` file, select your microSD card, and flash.

:::{warning}
Balena Etcher can also be used, but historically has had issues such as bootlooping (the system will repeatedly boot and restart) when imaging your device. Use at your own risk.
:::

## Limelight Installation

:::{note}
In order to mount the Limelight 4 on your computer, it's necessary to use `rpiboot`. To do this, follow the instructions [here](https://docs.limelightvision.io/docs/docs-limelight/getting-started/limelight-4#4-updating-limelightos).
:::

Limelights have a different installation processes. Simply connect the limelight to your computer using the proper usb cable. Select the compute module in the [Raspberry Pi Imager](https://www.raspberrypi.com/software/). If it doesn’t show up after 30s try using another USB port, initialization may take a while. If prompted, install the recommended missing drivers. Select the image, and flash.

:::{note}
Limelight 2, 2+, and 3 will need a [custom hardware config file](https://github.com/PhotonVision/photonvision/tree/main/docs/source/docs/advanced-installation/sw_install/files) for lighting to work.
:::

## Rubik Pi 3 Installation

:::{warning}
The Qualcomm Launcher caches files. If you flash multiple times, you may need to clear the cache by navigating to your temp directory, and deleting the `qualcomm-launcher` folder.
:::

To flash the Rubik Pi 3 coprocessor, it's necessary to use the [Qualcomm Launcher](https://softwarecenter.qualcomm.com/catalog/item/Qualcomm_Launcher). Upload a custom image by selecting the *Custom* option in the launcher. Choose the downloaded PhotonVision `.tar.xz` file and follow the prompts to complete the installation. It is recommended to skip the *Configure Login* process, as PhotonVision will handle the necessary settings.
