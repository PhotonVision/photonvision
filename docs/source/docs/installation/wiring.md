# Wiring

## Off-Robot Wiring

Plugging your coprocessor into the wall via a power brick will suffice for off robot wiring.

:::{note}
Please make sure your chosen power supply can provide enough power for your coprocessor. Undervolting (where enough power isn't being supplied) can cause many issues.
:::

## On-Robot Wiring

:::{note}
We recommend users use the [SnakeEyes Pi Hat](https://www.playingwithfusion.com/productview.php?pdid=133) as it provides passive power over ethernet (POE) and other useful features to simplify wiring and make your life easier.
:::

### Recommended: Coprocessor with Passive POE (Gloworm, Pi with SnakeEyes, Limelight)

1. Plug the [passive POE injector](https://www.revrobotics.com/rev-11-1210/) into the coprocessor and wire it to PDP/PDH (NOT the VRM).
2. Add a breaker to relevant slot in your PDP/PDH
3. Run an ethernet cable from the passive POE injector to your network switch / radio (we *STRONGLY* recommend the usage of a network switch, see the [networking](networking.md) section for more info.)

### Coprocessor without Passive POE

1a. Option 1: Get a micro USB (may be USB-C if using a newer Pi) pigtail cable and connect the wire ends to a regulator like [this](https://www.pololu.com/product/4082). Then, wire the regulator into your PDP/PDH and the Micro USB / USB C into your coprocessor.

1b. Option 2: Use a USB power bank to power your coprocessor. Refer to this year's robot rulebook on legal implementations of this.

2. Run an ethernet cable from your Pi to your network switch / radio (we *STRONGLY* recommend the usage of a network switch, see the [networking](networking.md) section for more info.)

This diagram shows how to use the recommended regulator to power a coprocessor.

```{image} images/pololu-diagram.png
:alt: A flowchart-type diagram showing how to connect wires from the PDP or PDH to
:  the recommended voltage regulator and then a Coprocessor.
```

:::{note}
The regulator comes with optional screw terminals that may be used to connect the PDP/PDH and Coprocessor power wires if you do not wish to solder them.
:::

Once you have wired your coprocessor, you are now ready to install PhotonVision.
