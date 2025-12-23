package org.photonvision.common.hardware.metrics;

import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.ProtobufPublisher;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkUtils;
import org.photonvision.common.util.TimedTaskManager;
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
    private static NetworkIF managedIFace;

    private long[] oldTicks;
    private long lastNetworkUpdate = 0;

    private MetricsManager mm;

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

        // iFaces = hal.getNetworkIFs();
        managedIFace =
                getNetworkIfByName(
                        ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface);

        lastNetworkUpdate = System.currentTimeMillis();

        mm = new MetricsManager();
        mm.setConfig( ConfigManager.getInstance().getConfig().getHardwareConfig());
    }

    public static final String taskName = "SystemMonitorPublisher";

    public void startMonitor() {
        if (!TimedTaskManager.getInstance().taskActive(taskName)) {
            logger.debug("Starting SystemMonitor ...");
            TimedTaskManager.getInstance().addTask(taskName, this::compare, 2000, 5000);
        } else {
            logger.warn("SystemMonitor already running!");
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

    private NetworkIF getNetworkIfByName(String name) {
        lastNetworkUpdate = System.currentTimeMillis();
        for (var iFace : hal.getNetworkIFs()) {
            if (iFace.getName().equals(name)) {
                logger.debug("Monitoring network traffic on '" + iFace.getName() + "'");
                return iFace;
            }
        }
        logger.warn("Can't retrieve network interface '" + name + "'");
        return null;
    }

    public void dumpMetricsToLog() {
        var sb = new StringBuilder();
        sb.append("*** System Information ***");
        sb.append(System.lineSeparator());
        sb.append("Operating System: " + os.toString());
        sb.append(System.lineSeparator());
        sb.append("  System Uptime: " + FormatUtil.formatElapsedSecs(getUptime()));
        sb.append(System.lineSeparator());
        sb.append("  Elevated Privileges: " + os.isElevated());
        sb.append(System.lineSeparator());

        var computerSystem = hal.getComputerSystem();
        sb.append("System: " + computerSystem.toString());
        sb.append(System.lineSeparator());
        sb.append("  Manufacturer: " + computerSystem.getManufacturer());
        sb.append(System.lineSeparator());
        sb.append("  Firmware: " + computerSystem.getFirmware());
        sb.append(System.lineSeparator());
        sb.append("  Baseboard: " + computerSystem.getBaseboard());
        sb.append(System.lineSeparator());
        sb.append("  Model: " + computerSystem.getModel());
        sb.append(System.lineSeparator());
        sb.append("  Serial Number: " + computerSystem.getSerialNumber());
        sb.append(System.lineSeparator());

        sb.append("CPU Info: " + cpu.toString());
        sb.append(System.lineSeparator());
        sb.append("  Max Frequency: " + FormatUtil.formatHertz(cpu.getMaxFreq()));
        sb.append(System.lineSeparator());
        sb.append(
                "  Current Frequency: "
                        + Arrays.stream(cpu.getCurrentFreq())
                                .mapToObj(FormatUtil::formatHertz)
                                .collect(Collectors.joining(", ")));
        sb.append(System.lineSeparator());
        for (PhysicalProcessor core : cpu.getPhysicalProcessors()) {
            sb.append("  Core " + core.getPhysicalProcessorNumber() + " (" + core.getEfficiency() + ")");
            sb.append(System.lineSeparator());
        }
        var myProc = os.getCurrentProcess();
        sb.append("Current Process: " + myProc.getName() + ", PID: " + myProc.getProcessID());
        sb.append(System.lineSeparator());
        // sb.append("  Command Line: " + myProc.getCommandLine());
        sb.append("  Kernel Time: " + myProc.getKernelTime());
        sb.append(System.lineSeparator());
        sb.append("  User Time: " + myProc.getUserTime());
        sb.append(System.lineSeparator());
        sb.append("  Cumulative Load: " + myProc.getProcessCpuLoadCumulative());
        sb.append(System.lineSeparator());
        sb.append("  Up Time: " + myProc.getUpTime());
        sb.append(System.lineSeparator());
        sb.append("  Priority: " + myProc.getPriority());
        sb.append(System.lineSeparator());
        sb.append("  User: " + myProc.getUser());
        sb.append(System.lineSeparator());
        sb.append("  Threads: " + myProc.getThreadCount());
        sb.append(System.lineSeparator());

        sb.append("Network Interfaces");
        sb.append(System.lineSeparator());
        for (NetworkIF iFace : hal.getNetworkIFs()) {
            sb.append("  Interface: " + iFace.toString());
            sb.append(System.lineSeparator());
        }

        sb.append("Graphics Cards");
        for (GraphicsCard gc : hal.getGraphicsCards()) {
            sb.append("  Card: " + gc.toString());
            sb.append(System.lineSeparator());
        }
        logger.info(sb.toString());
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

    double totalMemory = -1.0;

    public double getTotalMemory() {
        if (totalMemory < 0) {
            totalMemory = mem.getTotal() / MB;
        }
        return totalMemory;
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
            managedIFace = getNetworkIfByName(ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface);
            if (managedIFace == null) {
                return new NetworkTraffic(-1, -1);
            }
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
        StringBuilder sb = new StringBuilder();
        double total = 0;

        sb.append("System Metrics Update:\n");
        total += timeIt(sb, () -> String.format("System Uptime: %d", getUptime()));
        total += timeIt(sb, () -> "CPU Load: " + Arrays.toString(cpu.getSystemLoadAverage(3)));
        total += timeIt(sb, () -> String.format("CPU Usage: %.2f%%", getCpuUsage()));
        total += timeIt(sb, () -> String.format("CPU Temperature: %.2f °C", getCpuTemperature()));
        total += timeIt(sb, () -> String.format("NPU Usage: %s", Arrays.toString(getNpuUsage())));
        total += timeIt(sb, () -> String.format("Used Disk: %.2f%%", getUsedDiskPct()));
        total +=
                timeIt(
                        sb, () -> String.format("Memory: %.0f / %.0f MB", getUsedMemory(), getTotalMemory()));
        total +=
                timeIt(sb, () -> String.format("GPU Memory: %.0f / %.0f MB", getGpuMemUtil(), getGpuMem()));
        total += timeIt(sb, () -> String.format("CPU Throttle: %s", getCpuThrottleReason()));
        total +=
                timeIt(
                        sb,
                        () -> {
                            var nt = getNetworkTraffic();
                            return String.format(
                                    "Data sent: %.0f Kbps, Data recieved: %.0f Kbps",
                                    nt.sent() / 1000, nt.recv() / 1000);
                        });
        sb.append(String.format("==========\n%7.3f ms\n", total));

        logger.info(sb.toString());
    }

    public void testSM() {
        StringBuilder sb = new StringBuilder();
        double total = 0;

        sb.append("SystemMetrics Test:\n");
        total += timeIt(sb, () -> String.format("System Uptime: %d", getUptime()));
        total += timeIt(sb, () -> String.format("CPU Usage: %.2f%%", getCpuUsage()));
        total += timeIt(sb, () -> String.format("CPU Temperature: %.2f °C", getCpuTemperature()));
        total += timeIt(sb, () -> String.format("NPU Usage: %s", Arrays.toString(getNpuUsage())));
        total += timeIt(sb, () -> String.format("Used Disk: %.2f%%", getUsedDiskPct()));
        total +=
                timeIt(
                        sb, () -> String.format("Memory: %.0f / %.0f MB", getUsedMemory(), getTotalMemory()));
        total +=
                timeIt(sb, () -> String.format("GPU Memory: %.0f / %.0f MB", getGpuMemUtil(), getGpuMem()));
        total += timeIt(sb, () -> String.format("CPU Throttle: %s", getCpuThrottleReason()));
        sb.append(String.format("==========\n%7.3f ms\n", total));

        logger.info(sb.toString());
    }

    public void testMM() {
        StringBuilder sb = new StringBuilder();
        double total = 0;

        sb.append("MetricsManager Test:\n");
        total += timeIt(sb, () -> String.format("System Uptime: %.0f", mm.getUptime()));
        total += timeIt(sb, () -> String.format("CPU Usage: %.2f%%", mm.getCpuUtilization()));
        total += timeIt(sb, () -> String.format("CPU Temperature: %.2f °C", mm.getCpuTemp()));
        total += timeIt(sb, () -> String.format("NPU Usage: %s", Arrays.toString(mm.getNpuUsage())));
        total += timeIt(sb, () -> String.format("Used Disk: %.2f%%", mm.getUsedDiskPct()));
        total +=
                timeIt(
                        sb, () -> String.format("Memory: %.0f / %.0f MB", mm.getRamUtil(), mm.getRamMem()));
        total +=
                timeIt(sb, () -> String.format("GPU Memory: %.0f / %.0f MB", mm.getGpuMemUtil(), mm.getGpuMem()));
        total += timeIt(sb, () -> String.format("CPU Throttle: %s", mm.getThrottleReason()));
        sb.append(String.format("==========\n%7.3f ms\n", total));

        logger.info(sb.toString());
    }

    public void compare() {
        testSM();
        testMM();
    }

    public double timeIt(StringBuilder sb, Supplier<String> source) {
        long start = System.nanoTime();
        String resp = source.get();
        var delta = (System.nanoTime() - start) / 1000000.0;
        sb.append(String.format(" %7.3f ms >> %s\n", delta, resp));
        return delta;
    }
}
