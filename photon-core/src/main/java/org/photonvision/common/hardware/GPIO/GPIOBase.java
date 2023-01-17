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

package org.photonvision.common.hardware.GPIO;

import java.util.Arrays;
import java.util.HashMap;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public abstract class GPIOBase {
    private static final Logger logger = new Logger(GPIOBase.class, LogGroup.General);
    private static final ShellExec runCommand = new ShellExec(true, true);

    protected static HashMap<String, String> commands =
            new HashMap<>() {
                {
                    put("setState", "");
                    put("shutdown", "");
                    put("dim", "");
                    put("blink", "");
                }
            };

    protected static String execute(String command) {
        try {
            runCommand.executeBashCommand(command);
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            return "";
        }
        return runCommand.getOutput();
    }

    public abstract int getPinNumber();

    public void setState(boolean state) {
        if (getPinNumber() != -1) {
            setStateImpl(state);
        }
    }

    protected abstract void setStateImpl(boolean state);

    public final void setOff() {
        setState(false);
    }

    public final void setOn() {
        setState(true);
    }

    public void togglePin() {
        setState(!getStateImpl());
    }

    public abstract boolean shutdown();

    public final boolean getState() {
        if (getPinNumber() != -1) {
            return getStateImpl();
        } else return false;
    }

    public abstract boolean getStateImpl();

    public final void blink(int pulseTimeMillis, int blinks) {
        if (getPinNumber() != -1) {
            blinkImpl(pulseTimeMillis, blinks);
        }
    }

    protected abstract void blinkImpl(int pulseTimeMillis, int blinks);

    public final void setBrightness(int brightness) {
        if (getPinNumber() != -1) {
            if (brightness > 100) brightness = 100;
            if (brightness < 0) brightness = 0;
            setBrightnessImpl(brightness);
        }
    }

    protected abstract void setBrightnessImpl(int brightness);
}
