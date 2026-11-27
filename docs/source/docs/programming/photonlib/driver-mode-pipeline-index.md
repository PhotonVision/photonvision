# Driver Mode and Pipeline Index/Latency

After {ref}`creating a PhotonCamera <docs/programming/photonlib/getting-target-data:Constructing a PhotonCamera>`, one can toggle Driver Mode and change the Pipeline Index of the vision program from robot code.

## Toggle Driver Mode

You can use `setDriverMode()`/`SetDriverMode()`/`setDriverMode()` (Java, C++, and Python respectively) to toggle driver mode from your robot program. Driver mode is an unfiltered / normal view of the camera to be used while driving the robot.

```{eval-rst}
.. tab-set-code::

    .. code-block:: java

        // Set driver mode to on.
        camera.setDriverMode(true);

    .. code-block:: c++

        // Set driver mode to on.
        camera.SetDriverMode(true);

    .. code-block:: python

        # Set driver mode to on.
        camera.setDriverMode(True)
```

## Setting the Pipeline Index

You can use `setPipelineIndex()`/`SetPipelineIndex()`/`setPipelineIndex()` (Java, C++, and Python respectively) to dynamically change the vision pipeline from your robot program.

```{eval-rst}
.. tab-set-code::

    .. code-block:: java

        // Change pipeline to 2
        camera.setPipelineIndex(2);

    .. code-block:: c++

        // Change pipeline to 2
        camera.SetPipelineIndex(2);

    .. code-block:: python

        # Change pipeline to 2
        camera.setPipelineIndex(2)
```

## Getting the Pipeline Latency

You can also get the pipeline latency from a pipeline result using `getLatencyMillis()`/`GetLatency()`/`getLatencyMillis()` (Java, C++, and Python respectively) on a `PhotonPipelineResult`.

```{eval-rst}
.. tab-set-code::
   .. code-block:: java

      // Get the pipeline latency.
      double latencySeconds = result.getLatencyMillis() / 1000.0;

   .. code-block:: c++

      // Get the pipeline latency.
      wpi::units::second_t latency = result.GetLatency();

   .. code-block:: python

      # Get the pipeline latency.
      latencySeconds = result.getLatencyMillis() / 1000.0
```

:::{note}
The C++ version of PhotonLib returns the latency in a unit container. For more information on the Units library, see [here](https://docs.wpilib.org/en/stable/docs/software/basic-programming/cpp-units.html).
:::
