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

import com.diozero.api.DeviceMode;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.sbc.BoardPinInfo;
import com.diozero.sbc.DeviceFactoryHelper;
import edu.wpi.first.networktables.IntegerPublisher;
import edu.wpi.first.networktables.IntegerSubscriber;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.HardwareConfig;
import org.photonvision.common.configuration.HardwareSettings;
import org.photonvision.common.dataflow.networktables.NTDataChangeListener;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.gpio.CustomAdapter;
import org.photonvision.common.hardware.gpio.CustomDeviceFactory;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public class HardwareManager {
    private static HardwareManager instance;

    private final ShellExec shellExec = new ShellExec(true, false);
    private final Logger logger = new Logger(HardwareManager.class, LogGroup.General);

    private final HardwareConfig hardwareConfig;
    private final HardwareSettings hardwareSettings;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final StatusLED statusLED;

    @SuppressWarnings("FieldCanBeLocal")
    private final IntegerSubscriber ledModeRequest;

    private final IntegerPublisher ledModeState;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final NTDataChangeListener ledModeListener;

    public final VisionLED visionLED; // May be null if no LED is specified

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

        ledModeRequest =
                NetworkTablesManager.getInstance()
                        .kRootTable
                        .getIntegerTopic("ledModeRequest")
                        .subscribe(-1);
        ledModeState =
                NetworkTablesManager.getInstance().kRootTable.getIntegerTopic("ledModeState").publish();
        ledModeState.set(VisionLEDMode.kDefault.value);

        // Device factory is lazy to prevent creating one if it will go unused.
        Supplier<NativeDeviceFactoryInterface> lazyDeviceFactory =
                new Supplier<NativeDeviceFactoryInterface>() {
                    NativeDeviceFactoryInterface deviceFactory = null;

                    @Override
                    public NativeDeviceFactoryInterface get() {
                        if (deviceFactory == null) {
                            if (hardwareConfig.hasGPIOCommandsConfigured()) {
                                deviceFactory = HardwareManager.configureCustomGPIO(hardwareConfig);
                            } else {
                                deviceFactory = DeviceFactoryHelper.getNativeDeviceFactory();
                            }
                        }

                        return deviceFactory;
                    }
                };

        statusLED =
                hardwareConfig.statusRGBPins.size() == 3
                        ? new StatusLED(
                                lazyDeviceFactory.get(),
                                hardwareConfig.statusRGBPins,
                                hardwareConfig.statusRGBActiveHigh)
                        : null;

        var hasBrightnessRange = hardwareConfig.ledBrightnessRange.size() == 2;
        visionLED =
                hardwareConfig.ledPins.isEmpty()
                        ? null
                        : new VisionLED(
                                lazyDeviceFactory.get(),
                                hardwareConfig.ledPins,
                                hardwareConfig.ledsCanDim,
                                hasBrightnessRange ? hardwareConfig.ledBrightnessRange.get(0) : 0,
                                hasBrightnessRange ? hardwareConfig.ledBrightnessRange.get(1) : 100,
                                hardwareConfig.ledPWMFrequency,
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

    public static NativeDeviceFactoryInterface configureCustomGPIO(HardwareConfig hardwareConfig) {
        // Create a new adapter and device factory using the commands from hardwareConfig
        CustomAdapter adapter =
                new CustomAdapter(
                        hardwareConfig.getGPIOCommand,
                        hardwareConfig.setGPIOCommand,
                        hardwareConfig.setPWMCommand,
                        hardwareConfig.setPWMFrequencyCommand,
                        hardwareConfig.setPWMFrequencyCommand);
        CustomDeviceFactory deviceFactory = new CustomDeviceFactory(adapter);
        BoardPinInfo pinInfo = deviceFactory.getBoardPinInfo();

        // Populate pin info according to hardware config
        for (int pin : hardwareConfig.ledPins) {
            if (hardwareConfig.ledsCanDim) {
                pinInfo.addGpioPinInfo(pin, pin, List.of(DeviceMode.PWM_OUTPUT, DeviceMode.DIGITAL_OUTPUT));
            } else {
                pinInfo.addGpioPinInfo(pin, pin, List.of(DeviceMode.DIGITAL_OUTPUT));
            }
        }
        for (int pin : hardwareConfig.statusRGBPins) {
            pinInfo.addGpioPinInfo(pin, pin, List.of(DeviceMode.DIGITAL_OUTPUT));
        }

        return deviceFactory;
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

    private Set<String> pipelineTargets = new HashSet<String>();
    private boolean ntConnected = false;

    public void setTargetsVisibleStatus(String uniqueName, boolean hasTargets) {
        if (hasTargets) {
            pipelineTargets.add(uniqueName);
        } else {
            pipelineTargets.remove(uniqueName);
        }
        updateStatus();
    }

    public void setNTConnected(boolean isConnected) {
        ntConnected = isConnected;
        updateStatus();
    }

    public void setError(PhotonStatus status) {
        if (status == null || !status.isError()) {
            updateStatus();
        } else if (statusLED != null) {
            statusLED.setStatus(status);
        }
    }

    private void updateStatus() {
        if (statusLED == null) {
            return;
        }
        PhotonStatus status;
        boolean anyTarget = !pipelineTargets.isEmpty();
        if (ntConnected) {
            if (anyTarget) {
                status = PhotonStatus.NT_CONNECTED_TARGETS_VISIBLE;
            } else {
                status = PhotonStatus.NT_CONNECTED_TARGETS_MISSING;
            }
        } else {
            if (anyTarget) {
                status = PhotonStatus.NT_DISCONNECTED_TARGETS_VISIBLE;
            } else {
                status = PhotonStatus.NT_DISCONNECTED_TARGETS_MISSING;
            }
        }
        statusLED.setStatus(status);
    }
}
