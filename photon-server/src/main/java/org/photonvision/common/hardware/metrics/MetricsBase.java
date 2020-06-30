package org.photonvision.common.hardware.metrics;

import org.photonvision.common.util.ShellExec;

abstract class MetricsBase {

    private static final ShellExec runCommand = new ShellExec(true, true);

    public static double execute(String command) {
        try {
            runCommand.executeBashCommand(command);
            return Double.parseDouble(runCommand.getOutput());
        } catch (Exception e) {
            e.printStackTrace();
            return Double.NaN;
        }
    }
}
