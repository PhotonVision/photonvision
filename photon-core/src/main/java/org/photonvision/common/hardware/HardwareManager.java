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

import edu.wpi.first.networktables.IntegerPublisher;
import edu.wpi.first.networktables.IntegerSubscriber;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.configuration.HardwareSettings;
import org.photonvision.common.dataflow.networktables.NTDataChangeListener;
import org.photonvision.common.dataflow.networktables.NTDriverStation;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.GPIO.CustomGPIO;
import org.photonvision.common.hardware.GPIO.pi.PigpioSocket;
import org.photonvision.common.hardware.metrics.MetricsManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;
import org.photonvision.common.util.TimedTaskManager;

public class HardwareManager {
    private static HardwareManager instance;

    private final ShellExec shellExec = new ShellExec(true, false);
    private final Logger logger = new Logger(HardwareManager.class, LogGroup.General);

    private final HardwareConfig hardwareConfig;
    private final HardwareSettings hardwareSettings;

    public final MetricsManager metricsManager;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final StatusLED statusLED;

    @SuppressWarnings("FieldCanBeLocal")
    private final IntegerSubscriber ledModeRequest;

    private final IntegerPublisher ledModeState;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final NTDataChangeListener ledModeListener;

    public final VisionLED visionLED; // May be null if no LED is specified

    private final PigpioSocket pigpioSocket; // will be null unless on Raspi

    public static HardwareManager getInstance() {
        if (instance == null) {
            var conf = ConfigManager.getInstance().getConfig();
            instance = new HardwareManager(conf.getHardwareConfig(), conf.getHardwareSettings());
        }
        return instance;
    }

    private HardwareManager(HardwareConfig hardwareConfig, HardwareSettings hardwareSettings) {
        this.hardwareConfig = hardwareConfig;
        this.hardwareSettings = hardwareSettings;

        this.metricsManager = new MetricsManager();
        this.metricsManager.setConfig(hardwareConfig);

        TimedTaskManager.getInstance()
                .addTask("Metrics Publisher", this.metricsManager::publishMetrics, 5000);

        ledModeRequest =
                NetworkTablesManager.getInstance()
                        .kRootTable
                        .getIntegerTopic("ledModeRequest")
                        .subscribe(-1);
        ledModeState =
                NetworkTablesManager.getInstance().kRootTable.getIntegerTopic("ledModeState").publish();
        ledModeState.set(VisionLEDMode.kDefault.value);

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
            TimedTaskManager.getInstance().addTask("StatusLEDUpdate", this::statusLEDUpdate, 150);
        }

        var hasBrightnessRange = hardwareConfig.ledBrightnessRange.size() == 2;
        visionLED =
                hardwareConfig.ledPins.isEmpty()
                        ? null
                        : new VisionLED(
                                hardwareConfig.ledPins,
                                hasBrightnessRange ? hardwareConfig.ledBrightnessRange.get(0) : 0,
                                hasBrightnessRange ? hardwareConfig.ledBrightnessRange.get(1) : 100,
                                pigpioSocket,
                                ledModeState::set);

        ledModeListener =
                visionLED == null
                        ? null
                        : new NTDataChangeListener(
                                NetworkTablesManager.getInstance().kRootTable.getInstance(),
                                ledModeRequest,
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

        ConfigManager.getInstance().onJvmExit();
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

    // API's supporting status LEDs

    private Map<String, Boolean> pipelineTargets = new HashMap<String, Boolean>();
    private boolean ntConnected = false;
    private boolean systemRunning = false;
    private int blinkCounter = 0;

    public void setTargetsVisibleStatus(String uniqueName, boolean hasTargets) {
        pipelineTargets.put(uniqueName, hasTargets);
    }

    public void setNTConnected(boolean isConnected) {
        this.ntConnected = isConnected;
    }

    public void setRunning(boolean isRunning) {
        this.systemRunning = isRunning;
    }

    private void statusLEDUpdate() {
        // make blinky
        boolean blinky = ((blinkCounter % 3) > 0);

        // check if any pipeline has a visible target
        boolean anyTarget = false;
        for (var t : this.pipelineTargets.values()) {
            if (t) {
                anyTarget = true;
            }
        }

        if (this.systemRunning) {
            if (!this.ntConnected) {
                if (anyTarget) {
                    // Blue Flashing
                    statusLED.setRGB(false, false, blinky);
                } else {
                    // Yellow flashing
                    statusLED.setRGB(blinky, blinky, false);
                }
            } else {
                if (anyTarget) {
                    // Blue
                    statusLED.setRGB(false, false, blinky);
                } else {
                    // blinky green
                    statusLED.setRGB(false, blinky, false);
                }
            }
        } else {
            // Faulted, not running... blinky red
            statusLED.setRGB(blinky, false, false);
        }

        blinkCounter++;
    }

    public void publishMetrics() {
        metricsManager.publishMetrics();
    }

    /**
     * Ensures there is enough space for new recordings by clearing stored recordings to free up disk
     * space. This method should delete the oldest recordings, prioritizing recordings from matches
     * over practice sessions regardless of age.
     *
     * @param requestedSpace The amount of space to free in MB
     * @return true if enough space was cleared, false otherwise
     */
    public boolean reserveRecordingSpace(double requestedSpace) {
        if (metricsManager.getDiskSpaceAvailable() > requestedSpace * 1024) {
            return true; // Enough space already available
        }

        Path recordingsDir = ConfigManager.getInstance().getRecordingsDirectory().toPath();

        // Create a list of all the recordings
        ArrayList<Path> recordings = new ArrayList<>();
        try {
            if (!java.nio.file.Files.exists(recordingsDir)
                    || !java.nio.file.Files.isDirectory(recordingsDir)) {
                logger.error("Recordings directory does not exist");
                return false; // No recordings directory
            }

            try (var cameraStream = java.nio.file.Files.list(recordingsDir)) {
                cameraStream
                        .filter(java.nio.file.Files::isDirectory)
                        .forEach(
                                cameraDir -> {
                                    try (var recStream = java.nio.file.Files.list(cameraDir)) {
                                        recStream.filter(java.nio.file.Files::isDirectory).forEach(recordings::add);
                                    } catch (Exception e) {
                                        // skip unreadable camera folder
                                    }
                                });
            }
        } catch (Exception e) {
            return false; // Unable to list recordings
        }

        if (recordings.isEmpty()) {
            return false; // No recordings to delete
        }

        // Create practice list and match list
        ArrayList<Path> matchRecordings = new ArrayList<>();
        ArrayList<Path> practiceRecordings = new ArrayList<>();

        for (Path rec : recordings) {
            String dirName = rec.getFileName().toString().toLowerCase();
            if (dirName.startsWith("event")) {
                matchRecordings.add(rec);
            } else {
                practiceRecordings.add(rec);
            }
        }

        // Sort both lists based on solely the filename
        matchRecordings.sort(
                (a, b) ->
                        NTDriverStation.compareMatchData(
                                a.getFileName().toString(), b.getFileName().toString()));
        practiceRecordings.sort(
                (a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));

        while (requestedSpace * 1024
                > HardwareManager.getInstance().metricsManager.getDiskSpaceAvailable()) {
            Path toDelete = null;
            if (!practiceRecordings.isEmpty()) {
                toDelete = practiceRecordings.remove(0);
            } else if (!matchRecordings.isEmpty()) {
                toDelete = matchRecordings.remove(0);
            } else {
                return false; // No recordings left to delete
            }
            try {
                java.nio.file.Files.delete(toDelete);
            } catch (Exception e) {
                logger.error("Failed to delete recording: " + toDelete.toString(), e);
                return false;
            }
        }

        return true;
    }
}
