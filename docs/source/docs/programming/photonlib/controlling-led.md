# Controlling LEDs

You can control the vision LEDs of supported hardware via PhotonLib using `setLED()`/`SetLED()`/`setLEDMode()` (Java, C++, and Python respectively) on a `PhotonCamera` instance. An `VisionLEDMode` enum is provided in all three languages. These values include `kOff`, `kOn`, `kBlink`, and `kDefault`. `kDefault` uses the default LED value from the selected pipeline.

```{eval-rst}
.. tab-set-code::
   .. code-block:: java

      // Blink the LEDs.
      camera.setLED(VisionLEDMode.kBlink);

   .. code-block:: c++

      // Blink the LEDs.
      camera.SetLED(photonlib::VisionLEDMode::kBlink);

   .. code-block:: python

      # Blink the LEDs.
      from photonlibpy.photonCamera import VisionLEDMode

      camera.setLEDMode(VisionLEDMode.kBlink)
```
