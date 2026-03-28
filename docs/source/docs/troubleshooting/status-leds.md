---
myst:
  substitutions:
    led_loader: |
      ```{image} images/led.svg
      :height: 0
      ```
    led_green: |
      ```{raw} html
      <object data="../../_images/led.svg">
        <param name="onColor" value="limegreen"/>
      </object>
      ```
    led_solid_blue: |
      ```{raw} html
      <object data="../../_images/led.svg">
        <param name="onColor" value="blue"/>
        <param name="onTime" value="indefinite"/>
      </object>
      ```
    led_yellow: |
      ```{raw} html
      <object data="../../_images/led.svg">
        <param name="onColor" value="yellow"/>
      </object>
      ```
    led_blue: |
      ```{raw} html
      <object data="../../_images/led.svg">
        <param name="onColor" value="blue"/>
      </object>
      ```
    led_red: |
      ```{raw} html
      <object data="../../_images/led.svg">
        <param name="onColor" value="red"/>
      </object>
      ```
    led_off: |
      ```{raw} html
      <object data="../../_images/led.svg">
        <param name="onColor" value="transparent"/>
        <param name="onTime" value="indefinite"/>
      </object>
      ```
    led_fast_green: |
      ```{raw} html
      <object data="../../_images/led.svg">
        <param name="onColor" value="limegreen"/>
        <param name="onTime" value="75ms"/>
        <param name="offTime" value="75ms"/>
      </object>
      ```
    led_solid_yellow: |
      ```{raw} html
      <object data="../../_images/led.svg">
        <param name="onColor" value="yellow"/>
        <param name="onTime" value="indefinite"/>
      </object>
      ```
---

# Status LEDs

PhotonVision has support for multiple kinds of status LEDs. Make sure you reference the correct table for the type present on your hardware.

## RGB LED

 Color  | Flashing | Preview              | Status
--------|----------|:--------------------:|-----------------------------------------------
 Green  | Yes      | {{ led_green }}      | Running normally, no targets visible
 Blue   | No       | {{ led_solid_blue }} | Running normally, targets visible
 Yellow | Yes      | {{ led_yellow }}     | NT Disconnected, no targets visible
 Blue   | Yes      | {{ led_blue }}       | NT Disconnected, targets visible
 Red    | Yes      | {{ led_red }}        | Initializing or faulted, not running
 Off    | No       | {{ led_off }}        | No power or initialization fault, not running

## Green and Yellow LEDs

Used on Limelight 1, 2, 2+, 3, 3G, and 3A

Green and Yellow LED patterns may be active at the same time

 Color  | Pattern        | Preview                              | Status
--------|----------------|:------------------------------------:|-------------------------------------------------
 Green  | Slow Flashing  | {{ led_green }} {{ led_off }}        | No targets visible
 Green  | Quick Flashing | {{ led_fast_green }} {{ led_off }}   | Targets visible
 Yellow | Flashing       | {{ led_off }} {{ led_yellow }}       | NT Disconnected
 Yellow | Solid          | {{ led_off }} {{ led_solid_yellow }} | NT Connected
 Both   | Off            | {{ led_off }} {{ led_off }}          | No power, initializing, or faulted, not running

{{ led_loader }}
