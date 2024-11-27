# Orange Pi NVME SSD

MicroSD cards are not the best storage solution for the environments FRC robots face.
In particular, they are not secured to the orange pi very well.
Teams have had the microSD card pop out during competition, disabling their vision processor.

The Orange Pi supports an M.2 SSD that can be secured with a screw.
The two supported drive sizes are _2230_ and _2242_.
The mounting hole is 3.5 mm in diameter, so an M3 or #4 bolt will fit well.
You can also use a standard PCB standoff secured with a nut through the PCB hole.


## Installing PhotonVision on an SSD

The following instructions are taken from the `ubuntu-rockchip` [wiki](https://github.com/Joshua-Riek/ubuntu-rockchip/wiki/Ubuntu-24.04-LTS#install-u-boot-to-the-spi-flash).

### 1. Install U-Boot to the SPI Flash

Booting directly from a USB or NVMe requires flashing U-Boot to the SPI, simply enter the below command:

```
pi@photonvision:~$ sudo u-boot-install-mtd
```

### 2. Install PhotonVision onto an NVMe from Linux

This will copy the installation of PhotonVision from the SD card to the SSD.
```
pi@photonvision:~$ sudo ubuntu-rockchip-install /dev/nvme0n1
```

### 3. Shutdown
```
pi@photonvision:~$ sudo shutdown -h now
```

### 4. Remove SD card and turn on

Remove the microSD card from the Orange Pi, then power it back on.
It helps to connect the orange pi to a display during boot to verify the boot sequence.