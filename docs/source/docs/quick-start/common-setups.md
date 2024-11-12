# Common Hardware Setups

## Coprocessors

- Orange Pi 5 4GB
  - Able to process two object detection streams at once while also processing 1 to 2 apriltag streams at 1280x800 (30fps).
- Raspberry Pi 5 2GB
  - A good cheaper option. Doesn't support object detection. Able to process 2 apriltag streams at 1280x800 (30fps).

## SD Cards

- 8GB or larger micro SD card
  - Many teams have found that an industrial micro sd card are much more stable in competition. One example is the SanDisk industrial 16GB micro SD card.

## Cameras

- Apriltag

  - Innomaker or Arducam OV9281 UVC USB cameras.

- Object Detection

  - Arducam OV9782 works well with its global shutter,
  - Most other fixed-focus color UVC USB webcams.

- Driver Camera
  - OV9281
  - OV9782
  - Pi Camera Module V1 {ref}`(More setup info)<docs/hardware/picamconfig:Pi Camera Configuration>`
  - Most other fixed-focus UVC USB webcams

## Power

- Pololu S13V30F5 Regulator

  - Wide power range input. Recommended by many teams.

- Redux Robotics Zinc-V Regulator

  - Recently released for the 2025 season, offering reliable and easy integration.
