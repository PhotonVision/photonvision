package org.photonvision.common.hardware.GPIO;

import java.util.HashMap;
import org.photonvision.common.util.ShellExec;

public abstract class GPIOBase {
    public HashMap<String, String> commands =
            new HashMap<>() {
                {
                    put("setState", "");
                    put("blink", "");
                    put("pulse", "");
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

    public void setStateCommand(String command) {
        commands.replace("setState", command);
    }

    public void setBlinkCommand(String command) {
        commands.replace("blink", command);
    }

    public void setPulseCommand(String command) {
        commands.replace("pulse", command);
    }

    public void setShutdownCommand(String command) {
        commands.replace("shutdown", command);
    }

    public abstract void togglePin();

    public abstract void setLow();

    public abstract void setHigh();

    public abstract void setState(boolean state);

    public abstract void blink(long delay, long duration);

    public abstract void pulse(long duration, boolean blocking);

    public abstract boolean shutdown();

    public abstract boolean getState();
}
