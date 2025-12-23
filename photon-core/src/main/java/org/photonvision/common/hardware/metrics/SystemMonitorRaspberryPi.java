package org.photonvision.common.hardware.metrics;

import java.io.IOException;
import org.photonvision.common.util.ShellExec;

public class SystemMonitorRaspberryPi extends SystemMonitor {

    private final ShellExec runCommand = new ShellExec(true, true);

    @Override
    public String getCpuThrottleReason() {
        int state = 0;
        String output = vcgencmd("get_throttled");
        try {
            state = Integer.decode(output);
        } catch (NumberFormatException e) {
            logger.warn("Could not parse return value: " + output);
        }
        if ((state & 0x01) != 0) {
            return "LOW VOLTAGE";
        } else if ((state & 0x08) != 0) {
            return "HIGH TEMP";
        } else if ((state & 0x1) != 0) {
            return "Prev. Low Voltage";
        } else if ((state & 0x1) != 0) {
            return "Prev. High Temp";
        }
        return "None";
    }

    @Override
    public double getGpuMem() {
        String output = vcgencmd("get_mem gpu");
        if (!output.isBlank()) {
            return Integer.parseInt(output);
        }
        return -1.0;
    }

    @Override
    public double getGpuMemUtil() {
        String output = vcgencmd("get_mem malloc");
        if (!output.isBlank()) {
            return Integer.parseInt(output);
        }
        return -1.0;
    }

    private String vcgencmd(String cmd) {
        if (cmd.isBlank()) {
            return "";
        }
        String command = "vcgencmd " + cmd;
        try {
            runCommand.executeBashCommand(command, true, false);
            if (runCommand.getExitCode() != 0) {
                logger.error("Bad response from vcgencmd: " + runCommand.getOutput());
                return "";
            } else {
                return runCommand.getOutput().split("=")[1].replaceAll("[^\\d.]$", "");
            }
        } catch (IOException e) {
            logger.error("Could not run `vcgencmd`!", e);
            return "";
        }
    }
}
