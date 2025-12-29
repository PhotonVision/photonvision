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

import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.ProtobufPublisher;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Supplier;
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

    private record NetworkTraffic(double sentBitRate, double recvBitRate) {}

    ProtobufPublisher<DeviceMetrics> metricPublisher =
            NetworkTablesManager.getInstance()
                    .kRootTable
                    .getSubTable("/metrics")
                    .getProtobufTopic(CameraServerJNI.getHostname(), DeviceMetrics.proto)
                    .publish();

    private SystemInfo si;
    private CentralProcessor cpu;
    private OperatingSystem os;
    private GlobalMemory mem;
    private HardwareAbstractionLayer hal;
    private FileStore fs;

    private double totalMemory = -1.0;

    private double lastCpuLoad = 0;
    private long lastCpuUpdate = 0;
    private long[] oldTicks;

    private NetworkIF monitoredIFace = null;
    private long lastTrafficUpdate = 0;
    private long lastBytesSent = 0;
    private long lastBytesRecv = 0;
    private NetworkTraffic lastResult = new NetworkTraffic(0, 0);

    public boolean writeMetricsToLog = false;

    private MetricsManager mm;

    private final String taskName = "SystemMonitorPublisher";
    private final double minimumDeltaTime = 0.250; // seconds
    private final long mebi = (1024 * 1024);

    /**
     * Returns the singleton instance of SystemMonitor. Creates the instance, thereby initializing it,
     * on the first call.
     *
     * @return instance of SystemMonitor
     */
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
        logger.info("Starting SystemMonitor");
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LOADAVERAGE, true);
        GlobalConfig.set("oshi.os.linux.sensors.cpuTemperature.types", getThermalZoneTypes());

        si = new SystemInfo();
        hal = si.getHardware();
        os = si.getOperatingSystem();
        cpu = hal.getProcessor();
        mem = hal.getMemory();

        try {
            // get the filesystem for the directory photonvision is running in
            fs = Files.getFileStore(Path.of(""));
        } catch (IOException e) {
            logger.error("Couldn't get FileStore for " + Path.of(""));
            fs = null;
        }

        // initialize CPU monitoring
        oldTicks = cpu.getSystemCpuLoadTicks();

        // initialize network traffic monitoring
        selectNetworkIfByName(
                ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface);

        // for comparison, TODO: remove before merging with main
        mm = new MetricsManager();
        mm.setConfig(ConfigManager.getInstance().getConfig().getHardwareConfig());
    }

    /**
     * Returns a comma-separated list of addtional thermal zone types that should be checked to get
     * the CPU temperature on Unix systems. The temperature will be reported for the first temperature
     * zone with a type that mateches an item of this list. If the CPU temperature isn't being
     * reported correctly for a coprocessor, override this method to return a string with type
     * associated with the thermal zone for that comprocessor.
     *
     * @return String containing a comma-separated list of thermal zone types for reading CPU
     *     temperature
     */
    protected String getThermalZoneTypes() {
        // Find the thermal zone type by logging on to the coprocessor and running:
        //     `cat /sys/class/thermal/thermal_zone*/type`
        // This command will show the types for all thermal zones.
        //
        return GlobalConfig.get("oshi.os.linux.sensors.cpuTemperature.types");
    }

    /**
     * Starts the periodic system monitor that publishes performance metrics. The metrics are
     * published every 5 seconds after a 2.5 second startup delay. The monitor can only be started
     * once and repeated calls do nothing.
     */
    public void startMonitor() {
        startMonitor(2500, 5000);
    }

    /**
     * Starts the periodic system monitor that publishes performance metrics. The metrics are
     * published every millisUpdateInterval milliseconds. The monitor can only be started once and
     * repeated calls do nothing.
     *
     * @param millisUpdateInterval the time between updates in units of milliseconds
     */
    public void startMonitor(long millisUpdateInterval) {
        startMonitor(0, millisUpdateInterval);
    }

    /**
     * Starts the periodic system monitor that publishes performance metrics. The metrics are
     * published every millisUpdateInerval seconds after a millisStartDelay startup delay. The monitor
     * can only be started once and repeated calls do nothing.
     *
     * @param millisStartDelay the delay before the metrics are first published
     * @param millisUpdateInterval the time between updates in units of milliseconds
     */
    public void startMonitor(long millisStartDelay, long millisUpdateInterval) {
        if (!TimedTaskManager.getInstance().taskActive(taskName)) {
            logger.debug("Starting SystemMonitor ...");
            TimedTaskManager.getInstance()
                    .addTask(taskName, this::publishMetrics, millisStartDelay, millisUpdateInterval);
        } else {
            logger.warn("SystemMonitor already running!");
        }
    }

    private void publishMetrics() {
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

        var nt = this.getNetworkTraffic();
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
                        this.getUseableDiskSpace(),
                        this.getNpuUsage(),
                        this.getIpAddress(),
                        this.getUptime(),
                        nt.sentBitRate,
                        nt.recvBitRate);

        metricPublisher.set(metrics);

        if (writeMetricsToLog) {
            logMetrics(metrics);
        }

        DataChangeService.getInstance().publishEvent(OutgoingUIEvent.wrappedOf("metrics", metrics));
    }

    private void logMetrics(DeviceMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("System Metrics Update: ");
        sb.append(String.format("System Uptime: %.0f, ", metrics.uptime()));
        sb.append(String.format("CPU Usage: %.2f%%, ", metrics.cpuUtil()));
        sb.append(String.format("CPU Temperature: %.2f °C, ", metrics.cpuTemp()));
        sb.append(String.format("NPU Usage: %s, ", Arrays.toString(metrics.npuUsage())));
        sb.append(String.format("Used Disk: %.2f%%, ", metrics.diskUtilPct()));
        sb.append(String.format("Usable Disk Space: %.0f MB, ", metrics.diskUsableSpace() / mebi));
        sb.append(String.format("Memory: %.0f / %.0f MB, ", metrics.ramUtil(), metrics.ramMem()));
        sb.append(
                String.format("GPU Memory: %.0f / %.0f MB, ", metrics.gpuMemUtil(), metrics.gpuMem()));
        sb.append(
                String.format("CPU Throttle: %s, ", metrics.cpuThr().isBlank() ? "N/A" : metrics.cpuThr()));
        sb.append(
                String.format(
                        "Data sent: %.0f Kbps, Data recieved: %.0f Kbps",
                        metrics.sentBitRate() / 1000, metrics.recvBitRate() / 1000));
        logger.debug(sb.toString());
    }

    private void resetNetworkTraffic() {
        lastBytesSent = monitoredIFace.getBytesSent();
        lastBytesRecv = monitoredIFace.getBytesRecv();
        lastTrafficUpdate = System.currentTimeMillis();
    }

    private NetworkIF selectNetworkIfByName(String name) {
        if (name.isBlank() || monitoredIFace != null && monitoredIFace.getName().equals(name)) {
            return monitoredIFace;
        }
        for (var iFace : hal.getNetworkIFs()) {
            if (iFace.getName().equals(name)) {
                logger.debug("Monitoring network traffic on '" + name + "'");
                monitoredIFace = iFace;
                resetNetworkTraffic();
                return iFace;
            }
        }
        logger.warn("Can't monitor network interface '" + name + "'");
        return null;
    }

    /** Writes available information about the hardware to the log. */
    public void logSystemInformation() {
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
     * Returns the total space available (in bytes) for the filesystem where PhotonVision is running
     * (typicallay "/"). This doesn't report on other mounted filesystems, such as USB sticks.
     *
     * @return the number of bytes total, or -1 if the command fails
     */
    public long getTotalDiskSpace() {
        if (fs != null) {
            try {
                return fs.getTotalSpace();
            } catch (IOException e) {
                logger.error("Couldn't retrieve total disk space", e);
            }
        }
        return -1;
    }

    /**
     * Returns the free space available (in bytes) for the filesystem where PhotonVision is running
     * (typicallay "/"). This doesn't report on other mounted filesystems, such as USB sticks.
     *
     * @return the number of bytes available, or -1 if the command fails
     */
    public long getUseableDiskSpace() {
        if (fs != null) {
            try {
                return fs.getUsableSpace();
            } catch (IOException e) {
                logger.error("Couldn't retrieve usable disk space", e);
            }
        }
        return -1;
    }

    /**
     * Get the percentage of disk space used.
     *
     * @return The percentage of disk space used, or -1.0 if the command fails
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

    /**
     * Returns the total RAM
     *
     * @return total RAM in MB
     */
    public double getTotalMemory() {
        if (totalMemory < 0) {
            totalMemory = mem.getTotal() / mebi;
        }
        return totalMemory;
    }

    /**
     * Returns the amount of memory in use
     *
     * @return the used RAM in MB
     */
    public double getUsedMemory() {
        return (mem.getTotal() - mem.getAvailable()) / mebi;
    }

    /**
     * Returns the time since system boot in seconds
     *
     * @return the uptime in seconds
     */
    public long getUptime() {
        return os.getSystemUptime();
    }

    /**
     * The average load on the CPU from 0 to 100% since last called by using the tick counters.
     *
     * @return load on the cpu in %
     */
    public synchronized double getCpuUsage() {
        long now = System.currentTimeMillis();
        double dTime = (now - lastCpuUpdate) / 1000.0;
        if (dTime > minimumDeltaTime) {
            var newTicks = cpu.getSystemCpuLoadTicks();
            lastCpuLoad = 100 * cpu.getSystemCpuLoadBetweenTicks(oldTicks, newTicks);
            oldTicks = newTicks;
            lastCpuUpdate = now;
        }
        return lastCpuLoad;
    }

    /**
     * Return the npu usage, if available. Platforms with NPUs will need to override this method to
     * return a useful value.
     *
     * @return the NPU usage or an empty array if not available
     */
    public double[] getNpuUsage() {
        return new double[0];
    }

    /**
     * Return a description of the CPU throttle state, if available. Platforms that provide this
     * information will need to override this method to return a useful value.
     *
     * @return the CPU throttle state, or an empty String if not available.
     */
    public String getCpuThrottleReason() {
        return "";
    }

    /**
     * Return the total GPU memory in MB. This only runs once, as it won't change over time.
     *
     * @return The total GPU memory in MB, or -1.0 if not avaialable on this platform.
     */
    public double getGpuMem() {
        return -1.0;
    }

    /**
     * Return the GPU memory utilization as MBs.
     *
     * @return The GPU memory utilization in MBs, or -1.0 if not available on this platform.
     */
    public double getGpuMemUtil() {
        return -1.0;
    }

    /**
     * Return the IP address of the device.
     *
     * @return The IP address as a string, or an empty string if the command fails.
     */
    public String getIpAddress() {
        String dev = ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface;
        String addr = NetworkUtils.getIPAddresses(dev);
        return addr;
    }

    private synchronized NetworkTraffic getNetworkTraffic() {
        String activeIFaceName =
                ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface;
        var iFace = selectNetworkIfByName(activeIFaceName);
        if (iFace == null) {
            return new NetworkTraffic(-1, -1);
        }
        long now = System.currentTimeMillis();
        double dTime = (now - lastTrafficUpdate) / 1000.0;
        if (dTime > minimumDeltaTime) {
            // only update if it's been long enough since the last update
            // otherwise, return the last value
            iFace.updateAttributes();
            long bytesSent = iFace.getBytesSent();
            long bytesRecv = iFace.getBytesRecv();
            double sentBitRate = 8 * (bytesSent - lastBytesSent) / dTime;
            double recvBitRate = 8 * (bytesRecv - lastBytesRecv) / dTime;
            lastBytesSent = bytesSent;
            lastBytesRecv = bytesRecv;
            lastResult = new NetworkTraffic(sentBitRate, recvBitRate);
            lastTrafficUpdate = now;
        }
        return lastResult;
    }

    private void testSM() {
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

    private void testMM() {
        StringBuilder sb = new StringBuilder();
        double total = 0;

        sb.append("MetricsManager Test:\n");
        total += timeIt(sb, () -> String.format("System Uptime: %.0f", mm.getUptime()));
        total += timeIt(sb, () -> String.format("CPU Usage: %.2f%%", mm.getCpuUtilization()));
        total += timeIt(sb, () -> String.format("CPU Temperature: %.2f °C", mm.getCpuTemp()));
        total += timeIt(sb, () -> String.format("NPU Usage: %s", Arrays.toString(mm.getNpuUsage())));
        total += timeIt(sb, () -> String.format("Used Disk: %.2f%%", mm.getUsedDiskPct()));
        total +=
                timeIt(sb, () -> String.format("Memory: %.0f / %.0f MB", mm.getRamUtil(), mm.getRamMem()));
        total +=
                timeIt(
                        sb,
                        () -> String.format("GPU Memory: %.0f / %.0f MB", mm.getGpuMemUtil(), mm.getGpuMem()));
        total += timeIt(sb, () -> String.format("CPU Throttle: %s", mm.getThrottleReason()));
        sb.append(String.format("==========\n%7.3f ms\n", total));

        logger.info(sb.toString());
    }

    private void compare() {
        testSM();
        testMM();
    }

    /**
     * Measures the time (in ms) that a String Supplier takes. This can be used to compare different
     * ways of gathering the same metric.
     *
     * @param sb A StringBuilder used to collect the output from the supplier.
     * @param source A supplier that takes no arguments and returns a string.
     * @return The time (in ms) required to produce the output.
     */
    private double timeIt(StringBuilder sb, Supplier<String> source) {
        long start = System.nanoTime();
        String resp = source.get();
        var delta = (System.nanoTime() - start) / 1000000.0;
        sb.append(String.format(" %7.3f ms >> %s\n", delta, resp));
        return delta;
    }
}
