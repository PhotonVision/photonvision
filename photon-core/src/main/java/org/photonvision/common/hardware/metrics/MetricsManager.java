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

import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.ProtobufPublisher;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.hardware.metrics.cmds.CmdBase;
import org.photonvision.common.hardware.metrics.cmds.FileCmds;
import org.photonvision.common.hardware.metrics.cmds.LinuxCmds;
import org.photonvision.common.hardware.metrics.cmds.PiCmds;
import org.photonvision.common.hardware.metrics.cmds.QCS6490Cmds;
import org.photonvision.common.hardware.metrics.cmds.RK3588Cmds;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkUtils;
import org.photonvision.common.util.ShellExec;

public class MetricsManager {
    final Logger logger = new Logger(MetricsManager.class, LogGroup.General);

    CmdBase cmds;

    ProtobufPublisher<DeviceMetrics> metricPublisher =
            NetworkTablesManager.getInstance()
                    .kRootTable
                    .getSubTable("/metrics")
                    .getProtobufTopic(CameraServerJNI.getHostname(), DeviceMetrics.proto)
                    .publish();

    private final ShellExec runCommand = new ShellExec(true, true);

    public void setConfig(HardwareConfig config) {
        if (config.hasCommandsConfigured()) {
            cmds = new FileCmds();
        } else if (Platform.isRaspberryPi()) {
            cmds = new PiCmds(); // Pi's can use a hardcoded command set
        } else if (Platform.isRK3588()) {
            cmds = new RK3588Cmds(); // RK3588 chipset hardcoded command set
        } else if (Platform.isQCS6490()) {
            cmds = new QCS6490Cmds(); // QCS6490 chipset hardcoded command set
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

    /**
     * Get the CPU temperature in Celsius.
     *
     * @return The CPU temperature in Celsius, or -1.0 if the command fails or parsing fails.
     */
    public double getCpuTemp() {
        try {
            return Double.parseDouble(safeExecute(cmds.cpuTemperatureCommand));
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    /**
     * Get the CPU utilization as a percentage.
     *
     * @return The CPU utilization as a percentage, or -1.0 if the command fails or parsing fails.
     */
    public double getCpuUtilization() {
        try {
            return Double.parseDouble(safeExecute(cmds.cpuUtilizationCommand));
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    /**
     * Get the reason for CPU throttling, if applicable.
     *
     * @return A string describing the CPU throttle reason, or an empty string if the command fails.
     */
    public String getThrottleReason() {
        return safeExecute(cmds.cpuThrottleReasonCmd);
    }

    private double ramMemSave = -2.0;

    /**
     * Get the total RAM memory in MB. This only runs once, as it won't change over time.
     *
     * @return The total RAM memory in MB, or -1.0 if the command fails or parsing fails.
     */
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

    /**
     * Get the RAM utilization in MBs.
     *
     * @return The RAM utilization in MBs, or -1.0 if the command fails or parsing fails.
     */
    public double getRamUtil() {
        try {
            return Double.parseDouble(safeExecute(cmds.ramUtilCommand));
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private double gpuMemSave = -2.0;

    /**
     * Get the total GPU memory in MB. This only runs once, as it won't change over time.
     *
     * @return The total GPU memory in MB, or -1.0 if the command fails or parsing fails.
     */
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

    /**
     * Get the GPU memory utilization as MBs.
     *
     * @return The GPU memory utilization in MBs, or -1.0 if the command fails or parsing fails.
     */
    public double getGpuMemUtil() {
        try {
            return Double.parseDouble(safeExecute(cmds.gpuMemUtilCommand));
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    /**
     * Get the percentage of disk space used.
     *
     * @return The percentage of disk space used, or -1.0 if the command fails or parsing fails.
     */
    public double getUsedDiskPct() {
        try {
            return Double.parseDouble(safeExecute(cmds.diskUsageCommand));
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    // This is here so we don't spam logs if it fails
    boolean npuParseWarning = false;

    /**
     * Get the NPU usage as an array of doubles.
     *
     * @return An array of doubles representing NPU usage, or null if parsing fails.
     */
    public double[] getNpuUsage() {
        if (cmds.npuUsageCommand.isBlank()) {
            return new double[0];
        }
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
                usageDoubles = new double[0]; // Default to empty array if parsing fails
                break;
            }
        }
        return usageDoubles;
    }

    /**
     * Get the IP address of the device.
     *
     * @return The IP address as a string, or an empty string if the command fails.
     */
    public String getIpAddress() {
        String dev = ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface;
        String addr = NetworkUtils.getIPAddresses(dev);
        return addr;
    }

    /**
     * Get the uptime of the device in seconds.
     *
     * @return The uptime in seconds, or -1.0 if the command fails or parsing fails.
     */
    public double getUptime() {
        try {
            return Double.parseDouble(safeExecute(cmds.uptimeCommand));
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    public void publishMetrics() {
        // Check that the hostname hasn't changed
        if (!CameraServerJNI.getHostname()
                .equals(NetworkTable.basenameKey(metricPublisher.getTopic().getName()))) {
            logger.warn("Metrics publisher name does not match hostname! Reinitializing publisher...");
            metricPublisher.close();
            metricPublisher =
                    NetworkTablesManager.getInstance()
                            .kRootTable
                            .getSubTable("/metrics")
                            .getProtobufTopic(CameraServerJNI.getHostname(), DeviceMetrics.proto)
                            .publish();
        }

        var metrics =
                new DeviceMetrics(
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

        metricPublisher.set(metrics);

        DataChangeService.getInstance().publishEvent(OutgoingUIEvent.wrappedOf("metrics", metrics));
    }

    public synchronized String execute(String command) {
        try {
            runCommand.executeBashCommand(command, true, false);
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
