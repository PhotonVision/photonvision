package org.photonvision.common.hardware.metrics.cmds;

import org.photonvision.common.configuration.HardwareConfig;

public class CmdBase {
    // CPU
    public String cpuMemoryCommand = "";
    public String cpuTemperatureCommand = "";
    public String cpuUtilizationCommand = "";
    public String cpuThrottleReasonCmd = "";
    public String cpuUptimeCommand = "";
    // GPU
    public String gpuMemoryCommand = "";
    public String gpuMemUsageCommand = "";
    // RAM
    public String ramUsageCommand = "";
    // Disk
    public String diskUsageCommand = "";

    public void initCmds(HardwareConfig config) {
        return; // default - do nothing
    }
}
