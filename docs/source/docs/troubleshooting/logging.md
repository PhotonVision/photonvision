# Logging

:::{note}
Logging is very helpful when trying to debug issues within PhotonVision, as it allows us to see what is happening within the program after it is ran. Whenever reporting an issue to PhotonVision, we request that you include logs whenever possible.
:::

In addition to storing logs in timestamped files in the config directory, PhotonVision streams logs to the web dashboard. These logs can be viewed later by pressing the \` key. In this view, logs can be filtered by level or downloaded.

:::{note}
When the program first starts, it sends logs from startup to the client that first connects. This does not happen on subsequent connections.
:::

:::{note}
Logs are stored inside the {code}`photonvision_config/logs` directory. Exporting the settings ZIP will also download all old logs for further review.
:::

```{raw} html
<video width="85%" controls>
    <source src="../../_static/assets/logGui.mp4" type="video/mp4">
    Your browser does not support the video tag.
</video>
```
