# FPS Limiter

:::{warning}
When using the FPS limiter, it's important to disable it before a match begins.
:::

The FPS limiter can be used to lower the frames processed per second for a given camera. This is intended to be used for power-saving, particularly in the case of high FPS cameras with powerful coprocessors. The value passed to the function will indicate the frames per second that should be processed. A value of -1 should be passed to indicate that the FPS limiter should not restrict processing; this is the default behavior.

```{eval-rst}
.. tab-set-code::
   .. code-block:: java

      int limit = camera.getFPSLimit();

      camera.setFPSLimit(10);

      // This removes any previously set FPS limit.
      camera.setFPSLimit(-1);

   .. code-block:: c++

        int limit = camera.GetFPSLimit();

        camera.SetFPSLimit(10);

        // This removes any previously set FPS limit.
        camera.SetFPSLimit(-1);

   .. code-block:: python

        limit = camera.getFPSLimit()

        camera.setFPSLimit(10)

        # This removes any previously set FPS limit.
        camera.setFPSLimit(-1)
```
