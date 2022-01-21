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
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public abstract class MetricsBase {
    private static final Logger logger = new Logger(MetricsBase.class, LogGroup.General);
    // CPU
    public static String cpuMemoryCommand = "vcgencmd get_mem arm | grep -Eo '[0-9]+'";
    public static String cpuTemperatureCommand =
            "sed 's/.\\{3\\}$/.&/' <<< cat /sys/class/thermal/thermal_zone0/temp";
    public static String cpuUtilizationCommand =
            "top -bn1 | grep \"Cpu(s)\" | sed \"s/.*, *\\([0-9.]*\\)%* id.*/\\1/\" | awk '{print 100 - $1}'";

    // GPU
    public static String gpuMemoryCommand = "vcgencmd get_mem gpu | grep -Eo '[0-9]+'";
    public static String gpuMemUsageCommand = "vcgencmd get_mem malloc | grep -Eo '[0-9]+'";

    // RAM
    public static String ramUsageCommand = "free --mega | awk -v i=2 -v j=3 'FNR == i {print $j}'";

    // Disk
    public static String diskUsageCommand = "df ./ --output=pcent | tail -n +2";

    private static ShellExec runCommand = new ShellExec(true, true);

    public static void setConfig(HardwareConfig config) {
        if (Platform.isRaspberryPi()) return;
        cpuMemoryCommand = config.cpuMemoryCommand;
        cpuTemperatureCommand = config.cpuTempCommand;
        cpuUtilizationCommand = config.cpuUtilCommand;

        gpuMemoryCommand = config.gpuMemoryCommand;
        gpuMemUsageCommand = config.gpuMemUsageCommand;

        diskUsageCommand = config.diskUsageCommand;

        ramUsageCommand = config.ramUtilCommand;
    }

    public static synchronized String execute(String command) {
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
