# Common Hardware Setups

PhotonVision requires dedicated hardware, above and beyond a roboRIO. This page lists hardware that is frequently used with PhotonVision.

## Coprocessors

- Orange Pi 5 4GB
  - Supports up to 2 object detection streams, along with 2 AprilTag streams at 1280x800 (30fps).
- Raspberry Pi 5 2GB
  - Supports up to 2 AprilTag streams at 1280x800 (30fps).

:::{note}
The Orange Pi 5 is the only currently supported device for object detection.
:::

## SD Cards

- 8GB or larger micro SD card

:::{important}
Industrial grade SD cards from major manufacturers are recommended for robotics applications. For example: Sandisk SDSDQAF3-016G-I .
:::

## Cameras

Innomaker and Arducam are common manufacturers of hardware designed specifically for vision processing.

- AprilTag Detection
  - OV9281

- Object Detection
  - OV9782

- Driver Camera
  - OV9281
  - OV9782
  - Pi Camera Module V1 {ref}`(More setup info)<docs/hardware/picamconfig:Pi Camera Configuration>`

Feel free to get started with any color webcam you have sitting around.

## Power

- Pololu S13V30F5 Regulator
- Redux Robotics Zinc-V Regulator

See {ref}`(Selecting Hardware)<docs/hardware/selecting-hardware:Selecting Hardware>` for info on why these are recommended.
