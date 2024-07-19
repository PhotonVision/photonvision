Driver Mode and Pipeline Index/Latency
======================================

After :ref:`creating a PhotonCamera <docs/programming/photonlib/getting-target-data:Constructing a PhotonCamera>`, one can toggle Driver Mode and change the Pipeline Index of the vision program from robot code.

Toggle Driver Mode
------------------
You can use the ``setDriverMode()``/``SetDriverMode()`` (Java and C++ respectively) to toggle driver mode from your robot program. Driver mode is an unfiltered / normal view of the camera to be used while driving the robot.

.. tab-set-code::

    .. code-block:: java

        // Set driver mode to on.
        camera.setDriverMode(true);

    .. code-block:: C++

        // Set driver mode to on.
        camera.SetDriverMode(true);

Setting the Pipeline Index
--------------------------
You can use the ``setPipelineIndex()``/``SetPipelineIndex()`` (Java and C++ respectively) to dynamically change the vision pipeline from your robot program.

.. tab-set-code::

    .. code-block:: java

        // Change pipeline to 2
        camera.setPipelineIndex(2);

    .. code-block:: C++

        // Change pipeline to 2
        camera.SetPipelineIndex(2);

Getting the Pipeline Latency
----------------------------
You can also get the pipeline latency from a pipeline result using the ``getLatencyMillis()``/``GetLatency()`` (Java and C++ respectively) methods on a ``PhotonPipelineResult``.

.. tab-set-code::
   .. code-block:: java

      // Get the pipeline latency.
      double latencySeconds = result.getLatencyMillis() / 1000.0;

   .. code-block:: c++

      // Get the pipeline latency.
      units::second_t latency = result.GetLatency();

.. note:: The C++ version of PhotonLib returns the latency in a unit container. For more information on the Units library, see `here <https://docs.wpilib.org/en/stable/docs/software/basic-programming/cpp-units.html>`_.
