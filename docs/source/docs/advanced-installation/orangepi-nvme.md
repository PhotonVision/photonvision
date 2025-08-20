# Orange Pi NVME SSD

_Time to complete: 5 minutes_

microSD cards are not the best storage solution for the environments FRC robots face.
In particular, they are not secured to the Orange Pi very well.
Teams have had the microSD card pop out during competition, disabling their vision processor.

The Orange Pi supports an M.2 SSD that can be secured with a screw.
The two supported drive sizes are _2230_ and _2242_.
The mounting hole is 3.5 mm in diameter, so an M3 or #4 bolt will fit well.
You can also use a standard PCB standoff secured with a nut through the PCB hole.

:::{note}

Installing PhotonVision onto an SSD requires a bootable image installed on your Orange Pi.

Follow the {ref}`Quick-Start <docs/quick-start/quick-install:Quick Install>` instructions for installing the latest PhotonVision image onto your Orange Pi before continuing.

:::

:::{note}

You will need access to your Orange Pi's console.
You can access the console directly by plugging in a keyboard to a USB port and connecting the Orange Pi to an external monitor through its HDMI port, or {ref}`via SSH <docs/troubleshooting/unix-commands:SSH>`.

:::

## Installing PhotonVision on an SSD

The following instructions are taken from the `ubuntu-rockchip` [wiki](https://github.com/Joshua-Riek/ubuntu-rockchip/wiki/Ubuntu-24.04-LTS#install-u-boot-to-the-spi-flash). These only

### 0. Install the SSD into your Orange Pi

Install the NVME SSD into the M.2 slot on the bottom of the Orange Pi.
Secure with a nut and bolt or standoff.

Reboot the Orange Pi after installing.

### 1. Install U-Boot to the SPI Flash

Booting directly from a USB or NVMe requires flashing U-Boot to the SPI, simply enter the below command:

```bash
sudo u-boot-install-mtd
```

### 2. Install PhotonVision onto an NVMe from Linux

This will copy the installation of PhotonVision from the SD card to the SSD.

```bash
sudo ubuntu-rockchip-install /dev/nvme0n1
```

### 3. Shutdown

```bash
sudo shutdown -h now
```

### 4. Remove SD card and turn on

Remove the microSD card from the Orange Pi, then power it back on.
It helps to connect the Orange Pi to a display during boot to verify the boot sequence.
