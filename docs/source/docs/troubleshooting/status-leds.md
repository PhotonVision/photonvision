---
myst:
  substitutions:
    led_loader: |
      ```{image} images/led.svg
      :height: 0
      ```
    led: |
      ```{raw} html
      <svg class="led" height="30" width="30">
        <use href="../../_images/led.svg#led"/>
      </svg>
      ```
---
<!-- markdownlint-disable-next-line MD033 MD041 -->
<style>
  svg.led {
    --off-color: transparent;
    color: var(--on-color);
  }

  @keyframes led-blink {
    66% {
      color: var(--off-color);
    }
  }

  :not(.solid) > svg.led {
    animation: led-blink 0.45s steps(1) infinite;
  }

  @keyframes led-fast-blink {
    50% {
      color: var(--off-color);
    }
  }
  
  :not(.solid).fast > svg.led {
    animation-name: led-fast-blink;
    animation-duration: 150ms;
  }

  .green > svg.led {
    --on-color: limegreen;
  }
  .blue > svg.led {
    --on-color: blue;
  }
  .yellow > svg.led {
    --on-color: yellow;
  }
  .red > svg.led {
    --on-color: red;
  }

  .off > svg.led {
    color: var(--off-color);
  }
</style>

# Status LEDs

PhotonVision has support for multiple kinds of status LEDs. Make sure you reference the correct table for the type present on your hardware.

## RGB LED

 Color  | Flashing | Preview                   | Status
--------|----------|:-------------------------:|-----------------------------------------------
 Green  | Yes      | [{{ led }}]{.green}       | Running normally, no targets visible
 Blue   | No       | [{{ led }}]{.solid .blue} | Running normally, targets visible
 Yellow | Yes      | [{{ led }}]{.yellow}      | NT Disconnected, no targets visible
 Blue   | Yes      | [{{ led }}]{.blue}        | NT Disconnected, targets visible
 Red    | Yes      | [{{ led }}]{.red}         | Initializing or faulted, not running
 Off    | No       | [{{ led }}]{.off}         | No power or initialization fault, not running

## Green and Yellow LEDs

Used on Limelight 1, 2, 2+, 3, 3G, and 3A

Green and Yellow LED patterns may be active at the same time

 Color  | Pattern        | Preview                                       | Status
--------|----------------|:---------------------------------------------:|-------------------------------------------------
 Green  | Slow Flashing  | [{{ led }}]{.green} [{{ led }}]{.off}         | No targets visible
 Green  | Quick Flashing | [{{ led }}]{.fast .green} [{{ led }}]{.off}   | Targets visible
 Yellow | Flashing       | [{{ led }}]{.off} [{{ led }}]{.yellow}        | NT Disconnected
 Yellow | Solid          | [{{ led }}]{.off} [{{ led }}]{.solid .yellow} | NT Connected
 Both   | Off            | [{{ led }}]{.off} [{{ led }}]{.off}           | No power, initializing, or faulted, not running

{{ led_loader }}
