package org.photonvision.common.hardware.metrics;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class MetricsPublisher {
    private final HashMap<String, Double> metrics;
    private final Thread metricsThread;

    public static MetricsPublisher getInstance() {
        return Singleton.INSTANCE;
    }

    private MetricsPublisher() {
        var cpu = CPU.getInstance();
        var gpu = GPU.getInstance();
        var ram = RAM.getInstance();

        metrics = new HashMap<>();

        this.metricsThread =
                new Thread(
                        () -> {
                            var timer = new Timer();
                            timer.schedule(
                                    new TimerTask() {
                                        public void run() {
                                            metrics.put("cpuTemp", cpu.getTemp());
                                            metrics.put("cpuUtil", cpu.getUtilization());
                                            metrics.put("cpuMem", cpu.getMemory());
                                            metrics.put("gpuTemp", gpu.getTemp());
                                            metrics.put("gpuMem", gpu.getMemory());
                                            metrics.put("ramUtil", ram.getUsedRam());
                                            /*
                                            DataChangeService.getInstance()
                                                    .publishEvent(
                                                            new OutgoingUIEvent<>(UIUpdateType.BROADCAST, "metrics", metrics));
                                             */
                                        }
                                    },
                                    0,
                                    1000);
                        });
    }

    private void startThread() {
        metricsThread.start();
    }

    private static class Singleton {
        public static final MetricsPublisher INSTANCE = new MetricsPublisher();
    }

}
