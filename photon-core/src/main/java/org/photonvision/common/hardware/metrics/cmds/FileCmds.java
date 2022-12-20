package org.photonvision.common.hardware.metrics.cmds;

import org.photonvision.common.configuration.HardwareConfig;

public class FileCmds extends CmdBase {

    @Override
    public void initCmds(HardwareConfig config) {
        cpuMemoryCommand = config.cpuMemoryCommand;
        cpuTemperatureCommand = config.cpuTempCommand;
        cpuUtilizationCommand = config.cpuUtilCommand;
        cpuThrottleReasonCmd = config.cpuThrottleReasonCmd;
        cpuUptimeCommand = config.cpuUptimeCommand;

        gpuMemoryCommand = config.gpuMemoryCommand;
        gpuMemUsageCommand = config.gpuMemUsageCommand;

        diskUsageCommand = config.diskUsageCommand;

        ramUsageCommand = config.ramUtilCommand;
    }
}
