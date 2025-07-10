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
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.metrics.cmds.CmdBase;
import org.photonvision.common.hardware.metrics.cmds.FileCmds;
import org.photonvision.common.hardware.metrics.cmds.LinuxCmds;
import org.photonvision.common.hardware.metrics.cmds.PiCmds;
import org.photonvision.common.hardware.metrics.cmds.RK3588Cmds;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkUtils;
import org.photonvision.common.util.ShellExec;

public class MetricsManager {
    final Logger logger = new Logger(MetricsManager.class, LogGroup.General);

    CmdBase cmds;

    private final ShellExec runCommand = new ShellExec(true, true);

    public void setConfig(HardwareConfig config) {
        if (config.hasCommandsConfigured()) {
            cmds = new FileCmds();
        } else if (Platform.isRaspberryPi()) {
            cmds = new PiCmds(); // Pi's can use a hardcoded command set
        } else if (Platform.isRK3588()) {
            cmds = new RK3588Cmds(); // RK3588 chipset hardcoded command set
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

    public double getCpuTemp() {
        Double value;
        try {
            value = Double.parseDouble(safeExecute(cmds.cpuTemperatureCommand));
        } catch (NumberFormatException e) {
            value = -1.0;
        }
        return value;
    }

    public double getCpuUtilization() {
        Double value;
        try {
            value = Double.parseDouble(safeExecute(cmds.cpuUtilizationCommand));
        } catch (NumberFormatException e) {
            value = -1.0;
        }
        return value;
    }

    public String getThrottleReason() {
        return safeExecute(cmds.cpuThrottleReasonCmd);
    }

    // This is only ran once as it retrieves total memory, this will not change over the course of
    // time
    private double ramMemSave = -2.0;

    public double getRamMem() {
        if (ramMemSave == -2.0) {
            try {
                ramMemSave = Double.parseDouble(safeExecute(cmds.ramMemCommand));
            } catch (NumberFormatException e) {
                ramMemSave = -1.0;
            }
        }
        return ramMemSave;
    }

    // TODO: Output in MBs for consistency
    public double getRamUtil() {
        Double value;
        try {
            value = Double.parseDouble(safeExecute(cmds.ramUtilCommand));
        } catch (NumberFormatException e) {
            value = -1.0;
        }
        return value;
    }

    // This is only ran once as it retrieves total memory, this will not change over the course of
    // time
    private double gpuMemSave = -2.0;

    public double getGpuMem() {
        if (gpuMemSave == -2.0) {
            try {
                gpuMemSave = Double.parseDouble(safeExecute(cmds.gpuMemCommand));
            } catch (NumberFormatException e) {
                gpuMemSave = -1.0;
            }
        }
        return gpuMemSave;
    }

    public double getGpuMemUtil() {
        Double value;
        try {
            value = Double.parseDouble(safeExecute(cmds.gpuMemUtilCommand));
        } catch (NumberFormatException e) {
            value = -1.0;
        }
        return value;
    }

    public double getUsedDiskPct() {
        Double value;
        try {
            value = Double.parseDouble(safeExecute(cmds.diskUsageCommand));
        } catch (NumberFormatException e) {
            value = -1.0;
        }
        return value;
    }

    // This is here so we don't spam logs if it fails
    boolean npuParseWarning = false;

    public double[] getNpuUsage() {
        String[] usages = safeExecute(cmds.npuUsageCommand).split(",");
        double[] usageDoubles = new double[usages.length];
        for (int i = 0; i < usages.length; i++) {
            try {
                usageDoubles[i] = Double.parseDouble(usages[i]);
                npuParseWarning = false; // Reset warning if parsing succeeds
            } catch (NumberFormatException e) {
                if (!npuParseWarning) {
                    logger.error("Failed to parse NPU usage value: " + usages[i], e);
                    npuParseWarning = true;
                }
                usageDoubles = null; // Default to null if parsing fails
                break;
            }
        }
        return usageDoubles;
    }

    public String getIpAddress() {
        String dev = ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface;
        logger.debug("Requesting IP addresses for \"" + dev + "\"");
        String addr = NetworkUtils.getIPAddresses(dev);
        logger.debug("Got value \"" + addr + "\"");
        return addr;
    }

    public double getUptime() {
        Double value;
        try {
            value = Double.parseDouble(safeExecute(cmds.uptimeCommand));
        } catch (NumberFormatException e) {
            value = -1.0;
        }
        return value;
    }

    public DeviceMetrics getMetrics() {
        return new DeviceMetrics(
                this.getCpuTemp(),
                this.getCpuUtilization(),
                this.getThrottleReason(),
                this.getRamMem(),
                this.getRamUtil(),
                this.getGpuMem(),
                this.getGpuMemUtil(),
                this.getUsedDiskPct(),
                this.getNpuUsage(),
                this.getIpAddress(),
                this.getUptime());
    }

    public void publishMetrics() {
        logger.debug("Publishing Metrics...");
        var metrics = getMetrics();

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
                            + e
                            + sw);
            return "";
        }
    }
}
