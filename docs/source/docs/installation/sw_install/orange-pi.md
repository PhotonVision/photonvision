# Orange Pi Installation

## Downloading Linux Image

Starting in 2024, PhotonVision provides pre-configured system images for Orange Pi 5 devices.  Download the latest release of the PhotonVision Orange Pi 5 image (.xz file suffixed with `orangepi5.xz`) from the [releases page](https://github.com/PhotonVision/photonvision/releases). You do not need to extract the downloaded archive file. This image is configured with a `pi` user with password `raspberry`.

For an Orange Pi 4, download the latest release of the Armbian Bullseye CLI image from [here](https://armbian.tnahosting.net/archive/orangepi4/archive/Armbian_23.02.2_Orangepi4_bullseye_current_5.15.93.img.xz).

## Flashing the Pi Image

An 8GB or larger SD card is recommended.

Use the 1.18.11 version of [Balena Etcher](https://github.com/balena-io/etcher/releases/tag/v1.18.11) to flash an image onto a Orange Pi. Select the downloaded image file, select your microSD card, and flash.

For more detailed instructions on using Etcher, please see the [Etcher website](https://www.balena.io/etcher/).

:::{warning}
Using a version of Balena Etcher older than 1.18.11 may cause bootlooping (the system will repeatedly boot and restart) when imaging your Orange Pi. Updating to 1.18.11 will fix this issue.
:::

Alternatively, you can use the [Raspberry Pi Imager](https://www.raspberrypi.com/software/) to flash the image.

Select "Choose OS" and then "Use custom" to select the downloaded image file. Select your microSD card and flash.

:::{note}
If you are working on Linux, "dd" can be used in the command line to flash an image.
:::

If you're using an Orange Pi 5, that's it! Orange Pi 4 users will need to install PhotonVision (see below).

### Initial User Setup (Orange Pi 4 Only)

Insert the flashed microSD card into your Orange Pi and boot it up. The first boot may take a few minutes as the Pi expands the filesystem. Be sure not to unplug during this process.

Plug your Orange Pi into a display via HDMI and plug in a keyboard via USB once its powered up. For an Orange Pi 4, complete the initial set up which involves creating a root password and adding a user, as well as setting localization language. Additionally, choose “bash” when prompted.

## Installing PhotonVision (Orange Pi 4 Only)

From here, you can follow {ref}`this guide <docs/installation/sw_install/other-coprocessors:Installing Photonvision>`.
