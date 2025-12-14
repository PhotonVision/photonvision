# Deploying on Custom Hardware

## Configuration

By default, PhotonVision attempts to make minimal assumptions of the hardware it runs on. However, it may be configured to enable custom LED control, branding, and other functionality.

`hardwareConfig.json` is the location for this configuration. It is included when settings are exported, and can be uploaded as part of a .zip, or on its own.

## LED Support

When running on Linux, PhotonVision can use [diozero](https://www.diozero.com) to control IO pins. The mapping of which pins control which LED's is part of the hardware config. The illumination LED pins are active-high: set high when LED's are commanded on, and set low when commanded off.

```{eval-rst}
.. tab-set-code::
   .. code-block:: json

      {
        "ledPins" : [ 13 ],
        "ledsCanDim" : true,
        "ledBrightnessRange" : [ 0, 100 ],
        "ledPWMFrequency" : 0,
        "statusRGBPins" : [ ],
        "statusRGBActiveHigh" : false,
      }
```

:::{note}
No hardware boards with status RGB LED pins or non-dimming LED's have been tested yet. Please reach out to the development team if these features are desired, they can assist with configuration and testing.
:::

### GPIO Pinout

::::{tab-set}

:::{tab-item} Raspberry Pi

The following diagram shows the GPIO pin numbering of the 40-pin header on Raspberry Pi hardware, courtesy of [pinout.xyz](https://pinout.xyz). Compute modules use the pin numbering from their respective datasheet.

```{image} https://raw.githubusercontent.com/pinout-xyz/Pinout.xyz/master/resources/raspberry-pi-pinout.png
:alt: Raspberry Pi GPIO Pinout
```

:::
::::

### Custom GPIO

If your hardware does not support diozero's default provider, custom commands can be provided to interact with the GPIO lines. The examples below show what parameters are provided to each command, which can be used in any order or multiple times as needed.

```{eval-rst}
.. tab-set-code::
   .. code-block:: json

      {
        "getGPIOCommand" : "getGPIO {p}",
        "setGPIOCommand" : "setGPIO {p} {s}",
        "setPWMCommand" : "setPWM {p} {v}",
        "setPWMFrequencyCommand" : "setPWMFrequency {p} {f}",
        "releaseGPIOCommand" : "releseGPIO {p}",
      }
```

The following template strings are used to input parameters to the commands:

| Template | Parameter  | Values     |
| -------- | ---------- | ---------- |
| `{p}`    | pin number | integers   |
| `{s}`    | state      | true/false |
| `{v}`    | value      | 0.0-1.0    |
| `{f}`    | frequency  | integers   |

If you were using custom LED commands from 2025 or earlier and still need custom GPIO commands, they can likely be copied over. `ledSetCommand` can be reused as `setGPIOCommand`. `ledDimCommand` can be reused with edits as `setPWMCommand`, replacing any occurrences of `{v}` with `$(awk 'BEGIN{ print int({v}*100) }')` if your command requires integer percentages.

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
        "ledPins" : [2, 13],
        "ledsCanDim" : true,
        "ledBrightnessRange" : [ 0, 100 ],
        "ledPWMFrequency" : 0,
        "statusRGBPins" : [ ],
        "statusRGBActiveHigh" : false,
        "getGPIOCommand" : "getGPIO {p}",
        "setGPIOCommand" : "setGPIO {p} {s}",
        "setPWMCommand" : "setPWM {p} {v}",
        "setPWMFrequencyCommand" : "setPWMFrequency {p} {f}",
        "releaseGPIOCommand" : "releseGPIO {p}",
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
