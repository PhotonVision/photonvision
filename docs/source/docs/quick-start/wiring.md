# Wiring

## Coprocessor with regulator

1. **IT IS STRONGLY RECOMMENDED** to use one of the recommended power regulators to prevent vision from cutting out from voltage drops while operating the robot. We recommend wiring the regulator directly to the power header pins or using a locking USB C cable. In any case we recommend hot gluing the connector.

2. Run an ethernet cable from your Pi to your network switch / radio.

This diagram shows how to use the recommended regulator to power a coprocessor.

::::{tab-set}

:::{tab-item} Orange Pi Zinc V USB C

```{image} images/OrangePiZincUSBC.png
:alt: Wiring the opi5 to the pdp using the Redux Robotics Zinc V and usb c
```

:::

:::{tab-item} Orange Pi 5 Zinc V

```{image} images/OrangePiZinc.png
:alt: Wiring the opi5 to the pdp using the Redux Robotics Zinc V
```

:::

:::{tab-item} Orange Pi 5 Pololu S13V30F5

```{image} images/OrangePiPololu.png
:alt: Wiring the opi5 to the pdp using the Pololu S13V30F5
```

:::

:::{tab-item} Orange Pi 5 Pololu S13V30F5 Pigtail

```{image} images/OrangePiPololuPigtail.png
:alt: Wiring the opi5 to the pdp using the Pololu S13V30F5 and a usb c pigtail
```

:::

:::{tab-item} Raspberry Pi 5 Zinc V USB C

```{image} images/RPiZincUSBC.png
:alt: Wiring the RPI5 to the pdp using the Redux Robotics Zinc V and usb c
```

:::

:::{tab-item} Raspberry Pi 5 Zinc V

```{image} images/RPiZinc.png
:alt: Wiring the RPI5 to the pdp using the Redux Robotics Zinc V
```

:::

:::{tab-item} Raspberry Pi 5 Pololu S13V30F5

```{image} images/RPiPololu.png
:alt: Wiring the RPI5 to the pdp using the Pololu S13V30F5
```

:::

:::{tab-item} Raspberry Pi 5 Pololu S13V30F5 Pigtail

```{image} images/RPiPololuPigtail.png
:alt: Wiring the RPI5 to the pdp using the Pololu S13V30F5 and a usb c pigtail
```

:::

::::

Pigtails can be purchased from many sources we recommend [(USB C)](https://ctr-electronics.com/products/usb-type-c-wire-breakout?_pos=19&_sid=bf06b6a6b&_ss=r) [(Micro USB)](https://ctr-electronics.com/products/usb-micro-power-wire-breakout?pr_prod_strat=e5_desc&pr_rec_id=10bf36ce7&pr_rec_pid=7863771070637&pr_ref_pid=7863771103405&pr_seq=uniform)

## Coprocessor with Passive POE (Pi with SnakeEyes and Limelight)

1. Plug the [passive POE injector](https://www.revrobotics.com/rev-11-1210/) into the coprocessor and wire it to PDP/PDH (NOT the VRM).
2. Add a breaker to relevant slot in your PDP/PDH
3. Run an ethernet cable from the passive POE injector to your network switch / radio.

## Off-Robot Wiring

Plugging your coprocessor into the wall via a power brick will suffice for off robot wiring.

:::{note}
Please make sure your chosen power supply can provide enough power for your coprocessor. Undervolting (where enough power isn't being supplied) can cause many issues.
:::
