package org.photonvision.common.hardware.PWM;

import java.util.HashMap;
import org.photonvision.common.util.ShellExec;

public abstract class PWMBase {
    HashMap<String, String> commands =
            new HashMap<>() {
                {
                    put("setRate", "");
                    put("setRange", "");
                    put("shutdown", "");
                }
            };

    private static final ShellExec runCommand = new ShellExec(true, true);

    public static String execute(String command) {
        try {
            runCommand.executeBashCommand(command);
        } catch (Exception e) {
            return "";
        }
        return runCommand.getOutput();
    }

    public void setPwmRateCommand(String command) {
        commands.replace("setRate", command);
    }

    public void setPwmRangeCommand(String command) {
        commands.replace("setRange", command);
    }

    public abstract void setPwmRate(int rate);

    public abstract void setPwmRange(int range);

    public abstract int getPwmRate();

    public abstract int getPwmRange();

    public abstract boolean shutdown();
}
