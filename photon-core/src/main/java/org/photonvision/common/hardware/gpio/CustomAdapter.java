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

package org.photonvision.common.hardware.gpio;

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public class CustomAdapter {
    private static final Logger logger = new Logger(CustomAdapter.class, LogGroup.General);
    private static final ThreadLocal<ShellExec> runCommand =
            ThreadLocal.withInitial(() -> new ShellExec(true, true));

    protected final String getGPIOCommand;
    protected final String setGPIOCommand;
    protected final String setPWMCommand;
    protected final String setPWMFrequencyCommand;
    protected final String releaseGPIOCommand;

    public CustomAdapter(
            String getGPIOCommand,
            String setGPIOCommand,
            String setPWMCommand,
            String setPWMFrequencyCommand,
            String releaseGPIOCommand) {
        this.getGPIOCommand = getGPIOCommand;
        this.setGPIOCommand = setGPIOCommand;
        this.setPWMCommand = setPWMCommand;
        this.setPWMFrequencyCommand = setPWMFrequencyCommand;
        this.releaseGPIOCommand = releaseGPIOCommand;
    }

    protected static String execute(String command) {
        try {
            runCommand.get().executeBashCommand(command);
        } catch (Exception e) {
            logger.error("Exception caught running GPIO command \"" + command + "\"", e);
            return "";
        }
        return runCommand.get().getOutput();
    }

    public boolean getGPIO(PinIdentifier gpio) {
        return Boolean.parseBoolean(execute(getGPIOCommand.replace("{p}", gpio.toString())).trim());
    }

    public void setGPIO(PinIdentifier gpio, boolean state) {
        execute(setGPIOCommand.replace("{p}", gpio.toString()).replace("{s}", Boolean.toString(state)));
    }

    public void setPWM(PinIdentifier gpio, double value) {
        execute(setPWMCommand.replace("{p}", gpio.toString()).replace("{v}", Double.toString(value)));
    }

    public void setPwmFrequency(PinIdentifier gpio, int frequency) {
        execute(
                setPWMFrequencyCommand
                        .replace("{p}", gpio.toString())
                        .replace("{f}", Integer.toString(frequency)));
    }

    public void releaseGPIO(PinIdentifier gpio) {
        execute(releaseGPIOCommand.replace("{p}", gpio.toString()));
    }
}
