/*
 * Copyright (C) 2020 Photon Vision.
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

package org.photonvision.common.hardware;

import java.io.IOException;
import java.util.HashMap;

import edu.wpi.first.networktables.EntryNotification;
import edu.wpi.first.networktables.NetworkTableEntry;
import org.photonvision.common.ProgramStatus;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.dataflow.networktables.NTDataChangeListener;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.GPIO.GPIOBase;
import org.photonvision.common.hardware.GPIO.PiGPIO;
import org.photonvision.common.hardware.metrics.MetricsBase;
import org.photonvision.common.hardware.metrics.MetricsPublisher;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;
import org.photonvision.common.util.TimedTaskManager;

public class HardwareManager {
    private static HardwareManager instance;

    private final HashMap<Integer, GPIOBase> VisionLEDs = new HashMap<>();
    private final ShellExec shellExec = new ShellExec(true, false);
    private final Logger logger = new Logger(HardwareManager.class, LogGroup.General);

    private final HardwareConfig hardwareConfig;
    private final StatusLED statusLED;
    private final NetworkTableEntry ledModeEntry;
    private final NTDataChangeListener ledModeListener;

    private VisionLEDMode currentLedMode = VisionLEDMode.VLM_OFF;
    private int visionLedPercentage = 100;

    public static HardwareManager getInstance() {
        if (instance == null) {
            instance = new HardwareManager(ConfigManager.getInstance().getConfig().getHardwareConfig());
        }
        return instance;
    }

    public enum VisionLEDMode {
        VLM_OFF(0),
        VLM_ON(1),
        VLM_BLINK(2);

        public final int value;

        VisionLEDMode(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            switch (this) {
                case VLM_OFF: return "Off";
                case VLM_ON: return "On";
                case VLM_BLINK: return "Blink";
            }
            return "";
        }
    }

    private HardwareManager(HardwareConfig hardwareConfig) {
        this.hardwareConfig = hardwareConfig;
        CustomGPIO.setConfig(hardwareConfig);
        MetricsBase.setConfig(hardwareConfig);

        ledModeEntry = NetworkTablesManager.getInstance().kRootTable.getEntry("ledMode");
        ledModeEntry.setNumber(currentLedMode.value);
        ledModeListener = new NTDataChangeListener(ledModeEntry, this::onLedModeChange);

        hardwareConfig.ledPins.forEach(
                pin -> {
                    if (Platform.isRaspberryPi()) {
                        VisionLEDs.put(
                                pin,
                                new PiGPIO(pin, hardwareConfig.ledPWMFrequency, hardwareConfig.ledPWMRange.get(1)));
                    } else {
                        VisionLEDs.put(pin, new CustomGPIO(pin));
                    }
                }
        );

        statusLED = new StatusLED(hardwareConfig.statusRGBPins);

        // Start hardware metrics thread
        if (Platform.isLinux()) MetricsPublisher.getInstance().startTask();
    }

    private void onLedModeChange(EntryNotification entryNotification) {
        var newLedModeRaw = (int)entryNotification.value.getDouble();
        VisionLEDMode newLedMode = null;
        switch (newLedModeRaw) {
            case 0:
                newLedMode = VisionLEDMode.VLM_OFF;
                break;
            case 1:
                newLedMode = VisionLEDMode.VLM_ON;
                break;
            case 2:
                newLedMode = VisionLEDMode.VLM_BLINK;
                break;
            default:
                logger.warn("User supplied invalid LED mode, defaulting to nearest mode");
                if (newLedModeRaw < 0) newLedMode = VisionLEDMode.VLM_OFF;
                if (newLedModeRaw > 2) newLedMode = VisionLEDMode.VLM_BLINK;
        }

        logger.info("Changing LED mode from \"" + currentLedMode.toString() + "\" to \"" + newLedMode.toString() + "\"");

        setVisionLEDsInternal(newLedMode);
    }

    public void blinkVisionLEDs(int pulseTimeMillis, int blinks) {
        VisionLEDs.values().forEach(led -> led.blink(pulseTimeMillis, blinks));
    }

    public void setBrightnessPercentage(int percentage) {
        visionLedPercentage = percentage;
        VisionLEDs.values().forEach(led -> led.setBrightness(percentage));
    }

    public void setVisionLEDMode(VisionLEDMode ledMode) {
        ledModeEntry.setNumber(ledMode.value);
        setVisionLEDsInternal(ledMode);
    }

    private void setVisionLEDsInternal(VisionLEDMode ledMode) {
        var lastLedMode = currentLedMode;
        currentLedMode = ledMode;
        if (ledMode == VisionLEDMode.VLM_BLINK) {
            blinkVisionLEDs(250, 3);
            var blinkTime = 3 * 250 * 2; // 2x for on/off time
            TimedTaskManager.getInstance().addOneShotTask(() -> setVisionLEDsInternal(ledMode), blinkTime);
        } else {
            boolean on = ledMode == VisionLEDMode.VLM_ON;
            VisionLEDs.values().forEach(led -> {
                if (on && visionLedPercentage != 100) {
                    led.setBrightness(visionLedPercentage);
                } else {
                    led.setState(on);
                }
            });
        }
    }

    public boolean restartDevice() {
        try {
            return shellExec.executeBashCommand(hardwareConfig.restartHardwareCommand) == 0;
        } catch (IOException e) {
            logger.error("Could not restart device!", e);
            return false;
        }
    }

    public void setStatus(ProgramStatus status) {
        switch (status) {
            case UHOH:
                // red flashing, green off
                break;
            case RUNNING:
                // red solid, green off
                break;
            case RUNNING_NT:
                // red off, green solid
                break;
            case RUNNING_NT_TARGET:
                // red off, green flashing
                break;
        }
    }

    public HardwareConfig getConfig() {
        return hardwareConfig;
    }
}
