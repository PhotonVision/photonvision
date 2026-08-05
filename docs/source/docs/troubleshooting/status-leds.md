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
  /* colors */

  .green > svg.led, .green.led-bar {
    --on-color: limegreen;
  }
  .blue > svg.led, .blue.led-bar {
    --on-color: blue;
  }
  .yellow > svg.led, .yellow.led-bar {
    --on-color: yellow;
  }
  .red > svg.led, .red.led-bar {
    --on-color: red;
  }

  .anti-yellow > svg.led {
    --on-color: transparent;
    --off-color: yellow;
  }

  /* classic LEDS */
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

  @keyframes led-even-blink {
    50% {
      color: var(--off-color);
    }
  }

  :not(.solid).fast > svg.led {
    animation-name: led-even-blink;
    animation-duration: 150ms;
  }

  :not(.solid).error > svg.led {
    animation-name: led-even-blink;
    animation-duration: 0.90s;
  }

  .off > svg.led {
    color: var(--off-color);
  }

  /* LED bars */

  .led-bar {
    display: block;
    box-sizing: border-box;
    width: calc(20px * 11);
    height: 14px;
    border: 2px solid black;
    border-radius: 7px;
    overflow: clip;
    background: darkgrey;
    --off-color: transparent;
  }

  .led-bar:after {
    content: "";
    display: block;
    width: 100%;
    height: 100%;
    background: var(--on-color);
    animation: 2s infinite cubic-bezier(0.37, 0, 0.63, 1);
  }

  .led-bar.throb:after {
    animation-name: led-bar-throb;
  }

  @keyframes led-bar-throb {
    0%, 100% {
      background: var(--off-color);
    }
    50% {
      background: var(--on-color);
    }
  }

  .led-bar.phaser::after {
    content: "";
    background: linear-gradient(
      to right,
      transparent 15%,
      var(--on-color) 50%,
      transparent 65%
    );
    position: relative;
    left: 0%;
    background-repeat: no-repeat;
    animation-name: led-bar-phaser;
  }

  @keyframes led-bar-phaser {
    0%, 100% {
      left: -50%;
    }
    25% {
      transform: scaleX(1);
    }
    50% {
      left: 50%;
    }
    75% {
      transform: scaleX(-1);
    }
  }

  .led-bar.blink:after {
    animation-name: led-bar-blink;
    animation-timing-function: steps(1);
  }

  @keyframes led-bar-blink {
    60% {
      background: var(--off-color);
    }
  }

  .led-bar.off:after {
    content: initial;
  }

</style>

# Status LEDs

PhotonVision has support for multiple kinds of status LEDs. Make sure you reference the correct table for the type present on your hardware.

## Addressable LEDs

Used on Luma P2

This applies to all types of addressable LEDs (APA102/SK9822)

 Color  | Pattern | Preview                     | Status
--------|---------|:---------------------------:|-----------------------------------------------
 Green  | Phaser  | []{.led-bar .green .phaser} | Running normally, no targets visible
 Blue   | Solid   | []{.led-bar .blue}          | Running normally, targets visible
 Yellow | Throb   | []{.led-bar .yellow .throb} | NT Disconnected, no targets visible
 Blue   | Throb   | []{.led-bar .blue .throb}   | NT Disconnected, targets visible
 Red    | Blink   | []{.led-bar .red .blink}    | Initializing or faulted, not running
 Off    | N/A     | []{.led-bar .off}           | No power or initialization fault, not running

## RGB LEDs

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

 Color  | Pattern        | Preview                                                     | Status
--------|----------------|:-----------------------------------------------------------:|-------------------------------------------------
 Green  | Slow Flashing  | [{{ led }}]{.green} [{{ led }}]{.off}                       | No targets visible
 Green  | Quick Flashing | [{{ led }}]{.fast .green} [{{ led }}]{.off}                 | Targets visible
 Yellow | Flashing       | [{{ led }}]{.off} [{{ led }}]{.yellow}                      | NT Disconnected
 Yellow | Solid          | [{{ led }}]{.off} [{{ led }}]{.solid .yellow}               | NT Connected
 Both   | Alternating    | [{{ led }}]{.green .error} [{{ led }}]{.anti-yellow .error} | Initializing or faulted, not running
 Both   | Off            | [{{ led }}]{.off} [{{ led }}]{.off}                         | No power or initialization fault, not running

{{ led_loader }}
