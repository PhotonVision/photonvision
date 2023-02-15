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

package org.photonvision.common.hardware;

import edu.wpi.first.networktables.IntegerEntry;
import java.io.IOException;
import org.photonvision.common.ProgramStatus;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.configuration.HardwareSettings;
import org.photonvision.common.dataflow.networktables.NTDataChangeListener;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.GPIO.pi.PigpioSocket;
import org.photonvision.common.hardware.metrics.MetricsManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public class HardwareManager {
    private static HardwareManager instance;

    private final ShellExec shellExec = new ShellExec(true, false);
    private final Logger logger = new Logger(HardwareManager.class, LogGroup.General);

    private final HardwareConfig hardwareConfig;
    private final HardwareSettings hardwareSettings;

    private final MetricsManager metricsManager;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final StatusLED statusLED;

    @SuppressWarnings("FieldCanBeLocal")
    private final IntegerEntry ledModeEntry;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final NTDataChangeListener ledModeListener;

    public final VisionLED visionLED; // May be null if no LED is specified

    private final PigpioSocket pigpioSocket; // will be null unless on Raspi

    public static HardwareManager getInstance() {
        if (instance == null) {
            var conf = ConfigManager.getInstance().getConfig();

            // Ensure we've loaded a valid config before proceeding.
            // Currently this shsould only go active during unit tests.
            if (conf == null) {
                ConfigManager.getInstance().load();
                conf = ConfigManager.getInstance().getConfig();
            }

            instance = new HardwareManager(conf.getHardwareConfig(), conf.getHardwareSettings());
        }
        return instance;
    }

    private HardwareManager(HardwareConfig hardwareConfig, HardwareSettings hardwareSettings) {
        this.hardwareConfig = hardwareConfig;
        this.hardwareSettings = hardwareSettings;

        this.metricsManager = new MetricsManager();
        this.metricsManager.setConfig(hardwareConfig);

        CustomGPIO.setConfig(hardwareConfig);

        if (Platform.isRaspberryPi()) {
            pigpioSocket = new PigpioSocket();
        } else {
            pigpioSocket = null;
        }

        statusLED =
                hardwareConfig.statusRGBPins.size() == 3
                        ? new StatusLED(hardwareConfig.statusRGBPins)
                        : null;

        if (statusLED != null) {
            logger.debug("Configured 3 status LED's");
        } else {
            logger.debug("No Status LED configured");
        }

        var hasBrightnessRange = hardwareConfig.ledBrightnessRange.size() == 2;
        visionLED =
                hardwareConfig.ledPins.isEmpty()
                        ? null
                        : new VisionLED(
                                hardwareConfig.ledPins,
                                hasBrightnessRange ? hardwareConfig.ledBrightnessRange.get(0) : 0,
                                hasBrightnessRange ? hardwareConfig.ledBrightnessRange.get(1) : 100,
                                pigpioSocket);

        ledModeEntry =
                NetworkTablesManager.getInstance().kRootTable.getIntegerTopic("ledMode").getEntry(0);
        ledModeEntry.set(VisionLEDMode.kDefault.value);
        ledModeListener =
                visionLED == null
                        ? null
                        : new NTDataChangeListener(
                                NetworkTablesManager.getInstance().kRootTable.getInstance(),
                                ledModeEntry,
                                visionLED::onLedModeChange);

        Runtime.getRuntime().addShutdownHook(new Thread(this::onJvmExit));

        if (visionLED != null) {
            visionLED.setBrightness(hardwareSettings.ledBrightnessPercentage);
            visionLED.blink(85, 4); // bootup blink
        }

        // Start hardware metrics thread (Disabled until implemented)
        // if (Platform.isLinux()) MetricsPublisher.getInstance().startTask();
    }

    public void setBrightnessPercent(int percent) {
        if (percent != hardwareSettings.ledBrightnessPercentage) {
            hardwareSettings.ledBrightnessPercentage = percent;
            if (visionLED != null) visionLED.setBrightness(percent);
            ConfigManager.getInstance().requestSave();
            logger.info("Setting led brightness to " + percent + "%");
        }
    }

    private void onJvmExit() {
        logger.info("Shutting down LEDs...");
        if (visionLED != null) visionLED.setState(false);
        if (statusLED != null) statusLED.setRGB(false, false, false);
    }

    public boolean restartDevice() {
        if (Platform.isLinux()) {
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

    private boolean targetVisible = false;

    public void setStatus(ProgramStatus status) {
        if (statusLED != null) {
            switch (status) {
                case UHOH:
                    statusLED.setRGB(true, false, false);
                    break;
                case RUNNING:
                    if (targetVisible) {
                        statusLED.setRGB(false, true, true);
                    } else {
                        statusLED.setRGB(true, true, false);
                    }
                    break;
                case RUNNING_NT:
                    if (targetVisible) {
                        statusLED.setRGB(false, true, true);
                    } else {
                        statusLED.setRGB(false, true, false);
                    }
                    break;
            }
        }
    }

    public void setTargetVisible(boolean isVisible) {
        targetVisible = isVisible;
    }

    public HardwareConfig getConfig() {
        return hardwareConfig;
    }

    public void publishMetrics() {
        metricsManager.publishMetrics();
    }
}
