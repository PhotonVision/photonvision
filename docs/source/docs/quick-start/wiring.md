# Wiring

## Coprocessor with regulator

1. **IT IS STRONGLY RECOMMENDED** to use one of the recommended power regulators to prevent vision from cutting out from voltage drops while operating the robot. We recommend wiring the regulator directly to the power header pins or using a locking USB C cable. If neither of these work for you at least heavily hot glue the connector.

2. Run an ethernet cable from your Pi to your network switch / radio.

This diagram shows how to use the recommended regulator to power a coprocessor.

```{image} images/pololu-diagram.png
:alt: A flowchart-type diagram showing how to connect wires from the PDP or PDH to
:  the recommended voltage regulator and then a Coprocessor.
```

## Coprocessor with Passive POE (Pi with SnakeEyes and Limelight)

1. Plug the [passive POE injector](https://www.revrobotics.com/rev-11-1210/) into the coprocessor and wire it to PDP/PDH (NOT the VRM).
2. Add a breaker to relevant slot in your PDP/PDH
3. Run an ethernet cable from the passive POE injector to your network switch / radio.

## Off-Robot Wiring

Plugging your coprocessor into the wall via a power brick will suffice for off robot wiring.

:::{note}
Please make sure your chosen power supply can provide enough power for your coprocessor. Undervolting (where enough power isn't being supplied) can cause many issues.
:::
