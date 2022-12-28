/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.common.hardware.metrics;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.metrics.cmds.CmdBase;
import org.photonvision.common.hardware.metrics.cmds.FileCmds;
import org.photonvision.common.hardware.metrics.cmds.LinuxCmds;
import org.photonvision.common.hardware.metrics.cmds.PiCmds;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public class MetricsManager {
    final Logger logger = new Logger(MetricsManager.class, LogGroup.General);

    CmdBase cmds;

    private ShellExec runCommand = new ShellExec(true, true);

    public void setConfig(HardwareConfig config) {
        if (config.hasCommandsConfigured()) {
            cmds = new FileCmds();
        } else if (Platform.isRaspberryPi()) {
            cmds = new PiCmds(); // Pi's can use a hardcoded command set
        } else if (Platform.isLinux()) {
            cmds = new LinuxCmds(); // Linux/Unix platforms assume a nominal command set
        } else {
            cmds = new CmdBase(); // default - base has no commands
        }

        cmds.initCmds(config);
    }

    public String safeExecute(String str) {
        if (str.isEmpty()) return "";
        try {
            return execute(str);
        } catch (Exception e) {
            return "****";
        }
    }

    private String cpuMemSave = null;

    public String getMemory() {
        if (cmds.cpuMemoryCommand.isEmpty()) return "";
        if (cpuMemSave == null) {
            // save the value and only run it once
            cpuMemSave = execute(cmds.cpuMemoryCommand);
        }
        return cpuMemSave;
    }

    public String getTemp() {
        return safeExecute(cmds.cpuTemperatureCommand);
    }

    public String getUtilization() {
        return safeExecute(cmds.cpuUtilizationCommand);
    }

    public String getUptime() {
        return safeExecute(cmds.cpuUptimeCommand);
    }

    public String getThrottleReason() {
        return safeExecute(cmds.cpuThrottleReasonCmd);
    }

    private String gpuMemSave = null;

    public String getGPUMemorySplit() {
        if (gpuMemSave == null) {
            // only needs to run once
            gpuMemSave = safeExecute(cmds.gpuMemoryCommand);
        }
        return gpuMemSave;
    }

    public String getMallocedMemory() {
        return safeExecute(cmds.gpuMemUsageCommand);
    }

    public String getUsedDiskPct() {
        return safeExecute(cmds.diskUsageCommand);
    }

    // TODO: Output in MBs for consistency
    public String getUsedRam() {
        return safeExecute(cmds.ramUsageCommand);
    }

    public void publishMetrics() {
        logger.debug("Publishing Metrics...");
        final var metrics = new HashMap<String, String>();

        metrics.put("cpuTemp", this.getTemp());
        metrics.put("cpuUtil", this.getUtilization());
        metrics.put("cpuMem", this.getMemory());
        metrics.put("cpuThr", this.getThrottleReason());
        metrics.put("cpuUptime", this.getUptime());
        metrics.put("gpuMem", this.getGPUMemorySplit());
        metrics.put("ramUtil", this.getUsedRam());
        metrics.put("gpuMemUtil", this.getMallocedMemory());
        metrics.put("diskUtilPct", this.getUsedDiskPct());

        DataChangeService.getInstance().publishEvent(OutgoingUIEvent.wrappedOf("metrics", metrics));
    }

    public synchronized String execute(String command) {
        try {
            runCommand.executeBashCommand(command);
            return runCommand.getOutput();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            logger.error(
                    "Command: \""
                            + command
                            + "\" returned an error!"
                            + "\nOutput Received: "
                            + runCommand.getOutput()
                            + "\nStandard Error: "
                            + runCommand.getError()
                            + "\nCommand completed: "
                            + runCommand.isOutputCompleted()
                            + "\nError completed: "
                            + runCommand.isErrorCompleted()
                            + "\nExit code: "
                            + runCommand.getExitCode()
                            + "\n Exception: "
                            + e.toString()
                            + sw.toString());
            return "";
        }
    }
}
