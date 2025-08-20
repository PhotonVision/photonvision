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

import java.io.IOException;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.HardwareConfig;
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

    private final MetricsManager metricsManager;

    public static HardwareManager getInstance() {
        if (instance == null) {
            var conf = ConfigManager.getInstance().getConfig();
            instance = new HardwareManager(conf.getHardwareConfig());
        }
        return instance;
    }

    private HardwareManager(HardwareConfig hardwareConfig) {
        this.hardwareConfig = hardwareConfig;

        this.metricsManager = new MetricsManager();
        this.metricsManager.setConfig(hardwareConfig);

        TimedTaskManager.getInstance()
                .addTask("Metrics Publisher", this.metricsManager::publishMetrics, 5000);

        Runtime.getRuntime().addShutdownHook(new Thread(this::onJvmExit));
    }

    private void onJvmExit() {
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
            return shellExec.executeBashCommand(hardwareConfig.restartHardwareCommand()) == 0;
        } catch (IOException e) {
            logger.error("Could not restart device!", e);
            return false;
        }
    }

    public void publishMetrics() {
        metricsManager.publishMetrics();
    }
}
