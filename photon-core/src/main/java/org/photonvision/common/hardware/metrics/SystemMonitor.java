package org.photonvision.common.hardware.metrics;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkUtils;
import org.photonvision.common.util.TimedTaskManager;

import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.ProtobufPublisher;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.PhysicalProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import oshi.util.GlobalConfig;

public class SystemMonitor {
    protected static Logger logger = new Logger(SystemMonitor.class, LogGroup.General);
    
    private static SystemMonitor instance;
    private record NetworkTraffic(double sent, double recv) {}

    ProtobufPublisher<DeviceMetrics> metricPublisher =
            NetworkTablesManager.getInstance()
                    .kRootTable
                    .getSubTable("/metrics")
                    .getProtobufTopic(CameraServerJNI.getHostname(), DeviceMetrics.proto)
                    .publish();

    private static SystemInfo si;
    private static CentralProcessor cpu;
    private static OperatingSystem os;
    private static GlobalMemory mem;
    private static HardwareAbstractionLayer hal;
    private static FileStore fs;
    private static List<NetworkIF> iFaces;
    private static NetworkIF managedIFace;

    private long[] oldTicks;
    private long lastNetworkUpdate = 0;

    private static final long MB = (1024 * 1024);

    public static SystemMonitor getInstance() {
        if (instance == null) {
            if (Platform.isRaspberryPi()) {
                instance = new SystemMonitorRaspberryPi();
            } else if (Platform.isRK3588()) {
                instance = new SystemMonitorRK3588();
            } else {
                instance = new SystemMonitor();
            }
        }
        return instance;
    }

    protected SystemMonitor() {
        logger.info("Starting MetricsManagerOSHI");
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LOADAVERAGE, true);
        GlobalConfig.set(
                "oshi.os.linux.sensors.cpuTemperature.types",
                GlobalConfig.get("oshi.os.linux.sensors.cpuTemperature.types")
                        + ",bigcore0-thermal,cpu0-thermal");

        si = new SystemInfo();
        hal = si.getHardware();
        os = si.getOperatingSystem();
        cpu = hal.getProcessor();
        mem = hal.getMemory();

        try {
            // get the filesystem for the directory photonvision is running on
            fs = Files.getFileStore(Path.of(""));
        } catch (IOException e) {
            logger.error("Couldn't get FileStore for " + Path.of(""));
            fs = null;
        }
        oldTicks = cpu.getSystemCpuLoadTicks();
        lastNetworkUpdate = System.currentTimeMillis();

        iFaces = hal.getNetworkIFs();
        for (var iFace : iFaces) {
            if (iFace
                    .getName()
                    .equals(ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface)) {
                managedIFace = iFace;
                break;
            }
        }
        if (managedIFace != null) {
            logger.debug("Monitoring network traffic on '" + managedIFace.getName() + "'");
        } else {
            logger.warn(
                    "Can't monitor network interface '"
                            + ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface
                            + "'");
        }
    }

    public static final String taskName = "MetricsPublisher";

    public void startMonitor() {
        if (!TimedTaskManager.getInstance().taskActive(taskName)) {
            logger.debug("Starting SystemMonitor ...");
            TimedTaskManager.getInstance().addTask(taskName, this::publishMetrics, 2000, 5000);
        } else {
            logger.debug("SystemMonitor already running!");
        }
    }

    public void publishMetrics() {
        // Check that the hostname hasn't changed
        if (!CameraServerJNI.getHostname()
                .equals(NetworkTable.basenameKey(metricPublisher.getTopic().getName()))) {
            logger.warn("Metrics publisher name does not match hostname! Reinitializing publisher...");
            metricPublisher.close();
            metricPublisher =
                    NetworkTablesManager.getInstance()
                            .kRootTable
                            .getSubTable("/metrics")
                            .getProtobufTopic(CameraServerJNI.getHostname(), DeviceMetrics.proto)
                            .publish();
        }

        var metrics =
                new DeviceMetrics(
                        this.getCpuTemperature(),
                        this.getCpuUsage(),
                        this.getCpuThrottleReason(),
                        this.getTotalMemory(),
                        this.getUsedMemory(),
                        this.getGpuMem(),
                        this.getGpuMemUtil(),
                        this.getUsedDiskPct(),
                        this.getNpuUsage(),
                        this.getIpAddress(),
                        this.getUptime());

        metricPublisher.set(metrics);

        DataChangeService.getInstance().publishEvent(OutgoingUIEvent.wrappedOf("metrics", metrics));
    }

    public void dumpMetricsToLog() {
        logger.info("Operating System: " + os.toString());
        logger.info("  System Uptime: " + FormatUtil.formatElapsedSecs(getUptime()));
        logger.info("  Elevated Privileges: " + os.isElevated());

        var computerSystem = hal.getComputerSystem();
        logger.info("System: " + computerSystem.toString());
        logger.info("  Manufacturer: " + computerSystem.getManufacturer());
        logger.info("  Firmware: " + computerSystem.getFirmware());
        logger.info("  Baseboard: " + computerSystem.getBaseboard());
        logger.info("  Model: " + computerSystem.getModel());
        logger.info("  Serial Number: " + computerSystem.getSerialNumber());

        logger.info("CPU Info: " + cpu.toString());
        logger.info("  Max Frequency: " + FormatUtil.formatHertz(cpu.getMaxFreq()));
        logger.info(
                "  Current Frequency: "
                        + Arrays.stream(cpu.getCurrentFreq())
                                .mapToObj(FormatUtil::formatHertz)
                                .collect(Collectors.joining(", ")));
        for (PhysicalProcessor core : cpu.getPhysicalProcessors()) {
            logger.info(
                    "  Core " + core.getPhysicalProcessorNumber() + " (" + core.getEfficiency() + ")");
        }
        var myProc = os.getCurrentProcess();
        logger.info("Current Process: " + myProc.getName() + ", PID: " + myProc.getProcessID());
        // logger.info("  Command Line: " + myProc.getCommandLine());
        logger.info("  Kernel Time: " + myProc.getKernelTime());
        logger.info("  User Time: " + myProc.getUserTime());
        logger.info("  Cumulative Load: " + myProc.getProcessCpuLoadCumulative());
        logger.info("  Up Time: " + myProc.getUpTime());
        logger.info("  Priority: " + myProc.getPriority());
        logger.info("  User: " + myProc.getUser());
        logger.info("  Threads: " + myProc.getThreadCount());

        logger.info("Network Interfaces");
        for (NetworkIF iFace : hal.getNetworkIFs()) {
            logger.info("  Interface: " + iFace.toString());
        }

        logger.info("Graphics Cards");
        for (GraphicsCard gc : hal.getGraphicsCards()) {
            logger.info("  Card: " + gc.toString());
        }
    }

    /**
     * Get the percentage of disk space used.
     *
     * @return The percentage of disk space used, or -1.0 if the command fails.
     */
    public double getUsedDiskPct() {
        double usedPct;
        if (fs == null) return -1.0;
        try {
            double total = fs.getTotalSpace();
            // note: df matches better with fs.getUnallocatedSpace(), but this is more conservative
            usedPct = 100.0 * (1.0 - fs.getUsableSpace() / total);
        } catch (IOException e) {
            logger.error("Couldn't retrieve used disk space", e);
            usedPct = -1.0;
        }
        return usedPct;
    }

    /**
     * Get the temperature of the CPU
     *
     * @return The temperature of the CPU in °C or -1.0 if it cannot be retrieved
     */
    public double getCpuTemperature() {
        double temperature = hal.getSensors().getCpuTemperature();
        if (temperature < 0.1 || Double.isNaN(temperature)) {
            temperature = -1.0;
        }
        return temperature;
    }

    public double getTotalMemory() {
        return mem.getTotal() / MB;
    }

    public double getUsedMemory() {
        return (mem.getTotal() - mem.getAvailable()) / MB;
    }

    public long getUptime() {
        return os.getSystemUptime();
    }

    public double getCpuUsage() {
        // try { Thread.sleep(5000); } catch (Exception e) {}
        var newTicks = cpu.getSystemCpuLoadTicks();
        var cpuLoad = cpu.getSystemCpuLoadBetweenTicks(oldTicks, newTicks);
        oldTicks = newTicks;
        return 100.0 * cpuLoad;
    }

    public double[] getNpuUsage() {
        return new double[0];
    }

    public String getCpuThrottleReason() {
        return "";
    }

    /**
     * Get the total GPU memory in MB. This only runs once, as it won't change over time.
     *
     * @return The total GPU memory in MB, or -1.0 if not avaialable on this platform.
     */
    public double getGpuMem() {
        return -1.0;
    }

    /**
     * Get the GPU memory utilization as MBs.
     *
     * @return The GPU memory utilization in MBs, or -1.0 if not available on this platform.
     */
    public double getGpuMemUtil() {
        return -1.0;
    }

    /**
     * Get the IP address of the device.
     *
     * @return The IP address as a string, or an empty string if the command fails.
     */
    public String getIpAddress() {
        String dev = ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface;
        String addr = NetworkUtils.getIPAddresses(dev);
        return addr;
    }

    private NetworkTraffic getNetworkTraffic() {
        if (managedIFace == null) {
            return new NetworkTraffic(-1, -1);
        }
        long now = System.currentTimeMillis();
        double dTime = (now - lastNetworkUpdate) / 1000.0;
        if (dTime < 0.1) {
            // not enough time between calls
            return new NetworkTraffic(0, 0);
        }
        long lastBytesSent = managedIFace.getBytesSent();
        long lastBytesRecv = managedIFace.getBytesRecv();
        managedIFace.updateAttributes();
        long sent = managedIFace.getBytesSent() - lastBytesSent;
        long recv = managedIFace.getBytesRecv() - lastBytesRecv;
        lastNetworkUpdate = now;
        // multiply values by 8 to convert from bytes to bits
        return new NetworkTraffic(8 * sent / dTime, 8 * recv / dTime);
    }

    public void periodic() {
        long start = System.nanoTime();
        logger.info("System Metrics Update:");
        logger.debug(
                String.format("--> Operation took %.3f ms", (System.nanoTime() - start) / 1000000.0));
        start = System.nanoTime();
        logger.info("CPU Load: " + Arrays.toString(cpu.getSystemLoadAverage(3)));
        logger.debug(
                String.format("--> Operation took %.3f ms", (System.nanoTime() - start) / 1000000.0));
        start = System.nanoTime();
        logger.info(String.format("CPU Usage: %.2f%%", getCpuUsage()));
        logger.debug(
                String.format("--> Operation took %.3f ms", (System.nanoTime() - start) / 1000000.0));
        start = System.nanoTime();
        logger.info(String.format("CPU Temperature: %.2f °C", getCpuTemperature()));
        logger.debug(
                String.format("--> Operation took %.3f ms", (System.nanoTime() - start) / 1000000.0));
        start = System.nanoTime();
        logger.info(String.format("NPU Usage: %s", Arrays.toString(getNpuUsage())));
        logger.debug(
                String.format("--> Operation took %.3f ms", (System.nanoTime() - start) / 1000000.0));
        start = System.nanoTime();
        logger.info(String.format("Used Disk: %.2f%%", getUsedDiskPct()));
        logger.debug(
                String.format("--> Operation took %.3f ms", (System.nanoTime() - start) / 1000000.0));
        start = System.nanoTime();
        logger.info(String.format("Memory: %.0f / %.0f MB", getUsedMemory(), getTotalMemory()));
        logger.debug(
                String.format("--> Operation took %.3f ms", (System.nanoTime() - start) / 1000000.0));
        start = System.nanoTime();
        logger.info(String.format("GPU Memory: %.0f / %.0f MB", getGpuMemUtil(), getGpuMem()));
        logger.debug(
                String.format("--> Operation took %.3f ms", (System.nanoTime() - start) / 1000000.0));
        start = System.nanoTime();
        logger.info(String.format("CPU Throttle: %s", getCpuThrottleReason()));
        logger.debug(
                String.format("--> Operation took %.3f ms", (System.nanoTime() - start) / 1000000.0));
        start = System.nanoTime();
        var nt = getNetworkTraffic();
        logger.info(
                String.format(
                        "Data sent: %.0f Kbps, Data recieved: %.0f Kbps", nt.sent() / 1000, nt.recv() / 1000));
        logger.debug(
                String.format("--> Operation took %.3f ms", (System.nanoTime() - start) / 1000000.0));
    }
}
