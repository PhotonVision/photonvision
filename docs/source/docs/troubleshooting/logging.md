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

Robot mode transitions are also recorded in program logs. These transition messages look something like the two shown below, and show the contents of the [HAL Control Word](https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/hal/ControlWord.html) that the robot was in previously, and what it is now in. This includes:
- Enabled state
- Robot state (autonomous vs teleoperated)
- If the robot e-stop is active

If the robot is connected to the FMS at an event, we will additionally print out:
- Event name
- Match type and number
- Driver station position


```
[2025-04-19 19:52:08] [NetworkTables - NTDriverStation] [INFO] ROBOT TRANSITIONED MODES! From NtControlWord[m_enabled=true, m_autonomous=false, m_test=false, m_emergencyStop=false, m_fmsAttached=true, m_dsAttached=true] to NtControlWord[m_enabled=true, m_autonomous=false, m_test=true, m_emergencyStop=false, m_fmsAttached=true, m_dsAttached=true]

[2025-04-19 19:52:09] [NetworkTables - NTDriverStation] [INFO] ROBOT TRANSITIONED MODES! From NtControlWord[m_enabled=true, m_autonomous=false, m_test=true, m_emergencyStop=false, m_fmsAttached=true, m_dsAttached=true] to NtControlWord[m_enabled=false, m_autonomous=false, m_test=false, m_emergencyStop=false, m_fmsAttached=false, m_dsAttached=false]
[2025-04-19 19:52:19] [NetworkTables - NTDriverStation] [INFO] ROBOT TRANSITIONED MODES! From NtControlWord[m_enabled=false, m_autonomous=false, m_test=false, m_emergencyStop=false, m_fmsAttached=false, m_dsAttached=false] to NtControlWord[m_enabled=true, m_autonomous=true, m_test=false, m_emergencyStop=false, m_fmsAttached=true, m_dsAttached=true]
```
