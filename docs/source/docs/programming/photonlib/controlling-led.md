# Controlling LEDs

You can control the vision LEDs of supported hardware via PhotonLib using the `setLED()` method on a `PhotonCamera` instance. In Java and C++, an `VisionLEDMode` enum class is provided to choose values from. These values include, `kOff`, `kOn`, `kBlink`, and `kDefault`. `kDefault` uses the default LED value from the selected pipeline.

```{eval-rst}
.. tab-set-code::
   .. code-block:: Java

      // Blink the LEDs.
      camera.setLED(VisionLEDMode.kBlink);

   .. code-block:: C++

      // Blink the LEDs.
      camera.SetLED(photonlib::VisionLEDMode::kBlink);

   .. code-block:: Python

        # Coming Soon!
```
