package org.photonvision.common.hardware.GPIO;

import java.util.Arrays;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public class CustomAdapter {
    private static final Logger logger = new Logger(CustomAdapter.class, LogGroup.General);
    private static final ShellExec runCommand = new ShellExec(true, true);

    public final String deviceName;
    protected final String getGPIOCommand;
    protected final String setGPIOCommand;
    protected final String setPWMCommand;
    protected final String releaseGPIOCommand;

    public CustomAdapter(
            String deviceName,
            String getGPIOCommand,
            String setGPIOCommand,
            String setPWMCommand,
            String releaseGPIOCommand) {
        this.deviceName = deviceName;
        this.getGPIOCommand = getGPIOCommand;
        this.setGPIOCommand = setGPIOCommand;
        this.setPWMCommand = setPWMCommand;
        this.releaseGPIOCommand = releaseGPIOCommand;
    }

    protected static String execute(String command) {
        try {
            runCommand.executeBashCommand(command);
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            return "";
        }
        return runCommand.getOutput();
    }

    public boolean getGPIO(int gpio) {
        return Boolean.parseBoolean(
                execute(getGPIOCommand.replace("{p}", Integer.toString(gpio))).trim());
    }

    public void setGPIO(int gpio, boolean state) {
        execute(
                setGPIOCommand
                        .replace("{p}", Integer.toString(gpio))
                        .replace("{s}", Boolean.toString(state)));
    }

    public void setPWM(int gpio, double value) {
        execute(
                setPWMCommand
                        .replace("{p}", Integer.toString(gpio))
                        .replace("{v}", Double.toString(value)));
    }

    public void releaseGPIO(int gpio) {
        execute(releaseGPIOCommand.replace("{p}", Integer.toString(gpio)));
    }
}
