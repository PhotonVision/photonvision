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

package org.photonvision.common.hardware.metrics;

import java.util.HashMap;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;

public class MetricsPublisher {
    private static final Logger logger = new Logger(MetricsPublisher.class, LogGroup.General);
    private static CPUMetrics cpuMetrics;
    private static GPUMetrics gpuMetrics;
    private static RAMMetrics ramMetrics;
    private static DiskMetrics diskMetrics;

    public static MetricsPublisher getInstance() {
        return Singleton.INSTANCE;
    }

    private MetricsPublisher() {
        cpuMetrics = new CPUMetrics();
        gpuMetrics = new GPUMetrics();
        ramMetrics = new RAMMetrics();
        diskMetrics = new DiskMetrics();
    }

    public void stopTask() {
        TimedTaskManager.getInstance().cancelTask("Metrics");
        logger.info("This device does not support running bash commands. Stopped metrics thread.");
    }

    public void publish() {
        if (!Platform.isRaspberryPi()) {
            logger.debug("Ignoring metrics on non-Pi devices");
            return;
        }

        logger.debug("Publishing Metrics...");
        final var metrics = new HashMap<String, String>();

        metrics.put("cpuTemp", cpuMetrics.getTemp());
        metrics.put("cpuUtil", cpuMetrics.getUtilization());
        metrics.put("cpuMem", cpuMetrics.getMemory());
        metrics.put("gpuMem", gpuMetrics.getGPUMemorySplit());
        metrics.put("ramUtil", ramMetrics.getUsedRam());
        metrics.put("gpuMemUtil", gpuMetrics.getMallocedMemory());
        metrics.put("diskUtilPct", diskMetrics.getUsedDiskPct());

        var retMap = new HashMap<String, Object>();
        retMap.put("metrics", metrics);

        // TODO: MONOREPO - use pub/sub here
        //        try {
        //            SocketHandler.getInstance().broadcastMessage(retMap, null);
        //        } catch (JsonProcessingException e) {
        //            logger.error("Exception while sending metrics!", e);
        //        }
    }

    private static class Singleton {
        public static final MetricsPublisher INSTANCE = new MetricsPublisher();
    }
}
