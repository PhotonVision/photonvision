# Driver Mode and Pipeline Index/Latency

After {ref}`creating a PhotonCamera <docs/programming/photonlib/getting-target-data:Constructing a PhotonCamera>`, one can toggle Driver Mode and change the Pipeline Index of the vision program from robot code.

## Toggle Driver Mode

You can use the `setDriverMode()`/`SetDriverMode()` (Java and C++ respectively) to toggle driver mode from your robot program. Driver mode is an unfiltered / normal view of the camera to be used while driving the robot.

```{eval-rst}
.. tab-set-code::

    .. code-block:: java

        // Set driver mode to on.
        camera.setDriverMode(true);

    .. code-block:: c++

        // Set driver mode to on.
        camera.SetDriverMode(true);

    .. code-block:: python

        # Coming Soon!
```

## Setting the Pipeline Index

You can use the `setPipelineIndex()`/`SetPipelineIndex()` (Java and C++ respectively) to dynamically change the vision pipeline from your robot program.

```{eval-rst}
.. tab-set-code::

    .. code-block:: java

        // Change pipeline to 2
        camera.setPipelineIndex(2);

    .. code-block:: c++

        // Change pipeline to 2
        camera.SetPipelineIndex(2);

    .. code-block:: python

        # Coming Soon!
```

## Getting the Pipeline Image Capture Time

You can also get the pipeline's image capture time from a pipeline result using the `getTimestampSeconds()`/`GetTimestamp()` (Java and C++ respectively) methods on a `PhotonPipelineResult`. This is useful when adding vision pose estimates to a pose estimator or Kalman Filter, to help express the idea that camera image for the estimates was taken some time in the past.

```{eval-rst}
.. tab-set-code::
   .. code-block:: java

      // Get the pipeline image capture timestamp.
      double resultTime = result.getTimestampSeconds() / 1000.0;

   .. code-block:: c++

      // Get the pipeline image capture timestamp.
      units::second_t resultTime = result.GetTimestamp();

   .. code-block:: python

        # Coming Soon!
```

:::{note}
The C++ version of PhotonLib returns the image capture time in a unit container. For more information on the Units library, see [here](https://docs.wpilib.org/en/stable/docs/software/basic-programming/cpp-units.html).
:::
