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

import edu.wpi.first.networktables.NetworkTableEntry;
import java.io.IOException;
import org.photonvision.common.ProgramStatus;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.dataflow.networktables.NTDataChangeListener;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.VisionLED.VisionLEDMode;
import org.photonvision.common.hardware.metrics.MetricsBase;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public class HardwareManager {
    private static HardwareManager instance;

    private final ShellExec shellExec = new ShellExec(true, false);
    private final Logger logger = new Logger(HardwareManager.class, LogGroup.General);

    private final HardwareConfig hardwareConfig;

    @SuppressWarnings("FieldCanBeLocal")
    private final StatusLED statusLED;

    private final NetworkTableEntry ledModeEntry;

    @SuppressWarnings("FieldCanBeLocal")
    private final NTDataChangeListener ledModeListener;

    public final VisionLED visionLED;

    public static HardwareManager getInstance() {
        if (instance == null) {
            instance = new HardwareManager(ConfigManager.getInstance().getConfig().getHardwareConfig());
        }
        return instance;
    }

    private HardwareManager(HardwareConfig hardwareConfig) {
        this.hardwareConfig = hardwareConfig;
        CustomGPIO.setConfig(hardwareConfig);
        MetricsBase.setConfig(hardwareConfig);

        statusLED = new StatusLED(hardwareConfig.statusRGBPins);
        visionLED =
                new VisionLED(
                        hardwareConfig.ledPins,
                        hardwareConfig.ledPWMFrequency,
                        (hardwareConfig.ledPWMRange != null && hardwareConfig.ledPWMRange.size() == 2)
                                ? hardwareConfig.ledPWMRange.get(1)
                                : 0);

        ledModeEntry = NetworkTablesManager.getInstance().kRootTable.getEntry("ledMode");
        ledModeEntry.setNumber(VisionLEDMode.VLM_DEFAULT.value);
        ledModeListener = new NTDataChangeListener(ledModeEntry, visionLED::onLedModeChange);

        Runtime.getRuntime().addShutdownHook(new Thread(this::onJvmExit));

        // Start hardware metrics thread (Disabled until implemented)
        // if (Platform.isLinux()) MetricsPublisher.getInstance().startTask();
    }

    public void setBrightnessPercent(int percent) {
        ConfigManager.getInstance().getConfig().getHardwareConfig().enabledLEDPercentage = percent;
        visionLED.setBrightness(percent);
        ConfigManager.getInstance().requestSave();
        logger.info("Bright: " + percent);
    }

    private void onJvmExit() {
        logger.info("Shutting down...");
        visionLED.setState(false);
    }

    public boolean restartDevice() {
        if (Platform.isRaspberryPi()) {
            try {
                return shellExec.executeBashCommand("reboot now") == 0;
            } catch (IOException e) {
                logger.error("Could not restart device!", e);
                return false;
            }
        }
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
