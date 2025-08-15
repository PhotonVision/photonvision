# Deploying on Custom Hardware

## Configuration

By default, PhotonVision attempts to make minimal assumptions of the hardware it runs on. However, it may be configured to enable branding and other functionality.

`hardwareConfig.json` is the location for this configuration. It is included when settings are exported, and can be uploaded as part of a .zip, or on its own.

## Hardware Interaction Commands

For Non-Raspberry-Pi hardware, users must provide valid hardware-specific commands for some parts of the UI interaction (including performance metrics, and executing system restarts).

Leaving a command blank will disable the associated functionality.

```{eval-rst}
.. tab-set-code::
   .. code-block::  json

      {
        "cpuTempCommand" : "",
        "cpuMemoryCommand" : "",
        "cpuUtilCommand" : "",
        "gpuMemoryCommand" : "",
        "gpuTempCommand" : "",
        "ramUtilCommand" : "",
        "restartHardwareCommand" : "",
      }
```

:::{note}
These settings have no effect if PhotonVision detects it is running on a Raspberry Pi. See [the MetricsBase class](https://github.com/PhotonVision/photonvision/blob/dbd631da61b7c86b70fa6574c2565ad57d80a91a/photon-core/src/main/java/org/photonvision/common/hardware/metrics/MetricsBase.java) for the commands utilized.
:::

## Known Camera FOV

If your hardware contains a camera with a known field of vision, it can be entered into the hardware configuration. This will prevent users from editing it in the GUI.

```{eval-rst}
.. tab-set-code::
   .. code-block:: json

      {
        "vendorFOV" : 98.9
      }
```

## Cosmetic & Branding

To help differentiate your hardware from other solutions, some customization is allowed.

```{eval-rst}
.. tab-set-code::
   .. code-block:: json

      {
        "deviceName" : "Super Cool Custom Hardware",
        "deviceLogoPath" : "",
        "supportURL" : "https://cat-bounce.com/",
      }
```

:::{note}
Not all configuration is currently presented in the User Interface. Additional file uploads may be needed to support custom images.
:::

## Example

Here is a complete example `hardwareConfig.json`:

```{eval-rst}
.. tab-set-code::
   .. code-block:: json

      {
        "deviceName" : "Blinky McBlinkface",
        "deviceLogoPath" : "",
        "supportURL" : "https://www.youtube.com/watch?v=b-CvLWbfZhU",
        "cpuTempCommand" : "",
        "cpuMemoryCommand" : "",
        "cpuUtilCommand" : "",
        "gpuMemoryCommand" : "",
        "gpuTempCommand" : "",
        "ramUtilCommand" : "",
        "restartHardwareCommand" : "",
        "vendorFOV" : 72.5
      }
```
