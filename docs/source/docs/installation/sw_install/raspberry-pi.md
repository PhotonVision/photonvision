# Raspberry Pi Installation

A Pre-Built Raspberry Pi image is available for ease of installation.

## Downloading the Pi Image

Download the latest release of the PhotonVision Raspberry image (.xz file) from the [releases page](https://github.com/PhotonVision/photonvision/releases). You do not need to extract the downloaded ZIP file.

:::{note}
Make sure you download the image that ends in '-RaspberryPi.xz'.
:::

## Flashing the Pi Image

An 8GB or larger card is recommended.

Use the 1.18.11 version of [Balena Etcher](https://github.com/balena-io/etcher/releases/tag/v1.18.11) to flash an image onto a Raspberry Pi. Select the downloaded `.tar.xz` file, select your microSD card, and flash.

For more detailed instructions on using Etcher, please see the [Etcher website](https://www.balena.io/etcher/).

:::{warning}
Using a version of Balena Etcher older than 1.18.11 may cause bootlooping (the system will repeatedly boot and restart) when imaging your Raspberry Pi. Updating to 1.18.11 will fix this issue.
:::

Alternatively, you can use the [Raspberry Pi Imager](https://www.raspberrypi.com/software/) to flash the image.

Select "Choose OS" and then "Use custom" to select the downloaded image file. Select your microSD card and flash.

If you are using a non-standard Pi Camera connected to the CSI port, {ref}`additional configuration may be required. <docs/hardware/picamconfig:Pi Camera Configuration>`

## Final Steps

Insert the flashed microSD card into your Raspberry Pi and boot it up. The first boot may take a few minutes as the Pi expands the filesystem. Be sure not to unplug during this process.

After the initial setup process, your Raspberry Pi should be configured for PhotonVision. You can verify this by making sure your Raspberry Pi and computer are connected to the same network and navigating to `http://photonvision.local:5800` in your browser on your computer.

## Troubleshooting/Setting a Static IP

A static IP address may be used as an alternative to the mDNS `photonvision.local` address.

Download and run [Angry IP Scanner](https://angryip.org/download/#windows) to find PhotonVision/your coprocessor on your network.

```{image} images/angryIP.png
```

Once you find it, set the IP to a desired {ref}`static IP in PhotonVision. <docs/settings:Networking>`

## Updating PhotonVision

To upgrade a Raspberry Pi device with PhotonVision already installed, follow the {ref}`Raspberry Pi update instructions<docs/installation/updating:offline update>`.
