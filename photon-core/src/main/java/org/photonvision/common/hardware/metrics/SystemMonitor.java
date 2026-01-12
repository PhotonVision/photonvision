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
import org.photonvision.common.util.file.ProgramDirectoryUtilities;
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

    // Set this to true to enable logging the contents of the DeviceMetrics class that is sent to NT
    // and the UI.
    public boolean writeMetricsToLog = false;

    private final String taskName = "SystemMonitorPublisher";
    private final double minimumDeltaTime = 0.250; // seconds
    private final long mebi = (1024 * 1024);

    /**
     * Returns the singleton instance of SystemMonitor. Creates the instance, thereby initializing it,
     * on the first call.
     *
     * @return instance of SystemMonitor.
     */
    public static SystemMonitor getInstance() {
        if (instance == null) {
            if (Platform.isRaspberryPi()) {
                instance = new SystemMonitorRaspberryPi();
            } else if (Platform.isRK3588()) {
                instance = new SystemMonitorRK3588();
            } else if (Platform.isQCS6490()) {
                instance = new SystemMonitorQCS6490();
            } else if (Platform.isWindows()) {
                instance = new SystemMonitorWindows();
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
            fs = Files.getFileStore(Path.of(ProgramDirectoryUtilities.getProgramDirectory()));
        } catch (IOException e) {
            logger.error("Couldn't get FileStore for " + Path.of(""));
            fs = null;
        }

        // initialize CPU monitoring
        oldTicks = cpu.getSystemCpuLoadTicks();

        // initialize network traffic monitoring
        selectNetworkIfByName(
                ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface);
    }

    /**
     * Returns a comma-separated list of addtional thermal zone types that should be checked to get
     * the CPU temperature on Unix systems. The temperature will be reported for the first temperature
     * zone with a type that mateches an item of this list. If the CPU temperature isn't being
     * reported correctly for a coprocessor, override this method to return a string with type
     * associated with the thermal zone for that comprocessor.
     *
     * @return String containing a comma-separated list of thermal zone types for reading CPU
     *     temperature.
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
     * published every millisUpdateInerval seconds after a millisStartDelay startup delay. Calling
     * this method when the monitor is running will stop it and restart it with the new delay and
     * update interval.
     *
     * @param millisStartDelay the delay before the metrics are first published.
     * @param millisUpdateInterval the time between updates in units of milliseconds.
     */
    public void startMonitor(long millisStartDelay, long millisUpdateInterval) {
        if (TimedTaskManager.getInstance().taskActive(taskName)) {
            logger.debug("Stopping running SystemMonitor!");
            TimedTaskManager.getInstance().cancelTask(taskName);
        }
        logger.debug("Starting SystemMonitor with " + millisUpdateInterval + " ms update interval.");
        TimedTaskManager.getInstance()
                .addTask(taskName, this::publishMetrics, millisStartDelay, millisUpdateInterval);
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
                        this.getUsableDiskSpace(),
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
        sb.append(String.format("Usable Disk Space: %.0f MiB, ", metrics.diskUsableSpace() / mebi));
        sb.append(String.format("Memory: %.0f / %.0f MiB, ", metrics.ramUtil(), metrics.ramMem()));
        sb.append(
                String.format("GPU Memory: %.0f / %.0f MiB, ", metrics.gpuMemUtil(), metrics.gpuMem()));
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
        sb.append("*** System Information ***\n");
        sb.append("Operating System: " + os.toString() + "\n");
        sb.append("  System Uptime: " + FormatUtil.formatElapsedSecs(getUptime()) + "\n");
        sb.append("  Elevated Privileges: " + os.isElevated() + "\n");

        var computerSystem = hal.getComputerSystem();
        sb.append("System: " + computerSystem.toString() + "\n");
        sb.append("  Manufacturer: " + computerSystem.getManufacturer() + "\n");
        sb.append("  Firmware: " + computerSystem.getFirmware() + "\n");
        sb.append("  Baseboard: " + computerSystem.getBaseboard() + "\n");
        sb.append("  Model: " + computerSystem.getModel() + "\n");
        sb.append("  Serial Number: " + computerSystem.getSerialNumber() + "\n");

        sb.append("CPU Info: " + cpu.toString() + "\n");
        sb.append("  Max Frequency: " + FormatUtil.formatHertz(cpu.getMaxFreq()) + "\n");
        sb.append(
                "  Current Frequency: "
                        + Arrays.stream(cpu.getCurrentFreq())
                                .mapToObj(FormatUtil::formatHertz)
                                .collect(Collectors.joining(", "))
                        + "\n");
        for (PhysicalProcessor core : cpu.getPhysicalProcessors()) {
            sb.append(
                    "  Core " + core.getPhysicalProcessorNumber() + " (" + core.getEfficiency() + ")\n");
        }
        var myProc = os.getCurrentProcess();
        sb.append("Current Process: " + myProc.getName() + ", PID: " + myProc.getProcessID() + "\n");
        // sb.append("  Command Line: " + myProc.getCommandLine());
        sb.append("  Kernel Time: " + myProc.getKernelTime() + "\n");
        sb.append("  User Time: " + myProc.getUserTime() + "\n");
        sb.append("  Cumulative Load: " + myProc.getProcessCpuLoadCumulative() + "\n");
        sb.append("  Up Time: " + myProc.getUpTime() + "\n");
        sb.append("  Priority: " + myProc.getPriority() + "\n");
        sb.append("  User: " + myProc.getUser() + "\n");
        sb.append("  Threads: " + myProc.getThreadCount() + "\n");

        sb.append("Network Interfaces\n");
        for (NetworkIF iFace : hal.getNetworkIFs()) {
            sb.append("  Interface: " + iFace.toString() + "\n");
        }

        sb.append("Graphics Cards\n");
        for (GraphicsCard gc : hal.getGraphicsCards()) {
            sb.append("  Card: " + gc.toString() + "\n");
        }
        logger.info(sb.toString());
    }

    /**
     * Returns the total space available (in bytes) for the filesystem where PhotonVision is running
     * (typically "/"). This doesn't report on other mounted filesystems, such as USB sticks.
     *
     * @return the number of bytes total, or -1 if the command fails.
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
     * (typically "/"). This doesn't report on other mounted filesystems, such as USB sticks.
     *
     * @return the number of bytes available, or -1 if the command fails.
     */
    public long getUsableDiskSpace() {
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
     * Returns the percentage of disk space used.
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
     * Returns the temperature of the CPU.
     *
     * @return The temperature of the CPU in °C or -1.0 if it cannot be retrieved.
     */
    public double getCpuTemperature() {
        double temperature = hal.getSensors().getCpuTemperature();
        // OSHI returns 0 or NaN if the temperature isn't available.
        if (temperature == 0.0 || Double.isNaN(temperature)) {
            temperature = -1.0;
        }
        return temperature;
    }

    /**
     * Returns the total RAM.
     *
     * @return total RAM in MiB.
     */
    public double getTotalMemory() {
        if (totalMemory < 0) {
            totalMemory = mem.getTotal() / mebi;
        }
        return totalMemory;
    }

    /**
     * Returns the amount of memory in use.
     *
     * @return the used RAM in MiB.
     */
    public double getUsedMemory() {
        return (mem.getTotal() - mem.getAvailable()) / mebi;
    }

    /**
     * Returns the time since system boot in seconds.
     *
     * @return the uptime in seconds.
     */
    public long getUptime() {
        return os.getSystemUptime();
    }

    /**
     * Returns the average load on the CPU from 0 to 100% since last called by using the tick
     * counters.
     *
     * @return load on the cpu in %.
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
     * Returns the npu usage, if available. Platforms with NPUs will need to override this method to
     * return a useful value.
     *
     * @return the NPU usage or an empty array if not available.
     */
    public double[] getNpuUsage() {
        return new double[0];
    }

    /**
     * Returns a description of the CPU throttle state, if available. Platforms that provide this
     * information will need to override this method to return a useful value.
     *
     * @return the CPU throttle state, or an empty String if not available.
     */
    public String getCpuThrottleReason() {
        return "";
    }

    /**
     * Returns the total GPU memory in MiB.
     *
     * @return The total GPU memory in MiB, or -1.0 if not avaialable on this platform.
     */
    public double getGpuMem() {
        return -1.0;
    }

    /**
     * Returns the GPU memory utilization as MiBs.
     *
     * @return The GPU memory utilization in MiBs, or -1.0 if not available on this platform.
     */
    public double getGpuMemUtil() {
        return -1.0;
    }

    /**
     * Returns the IP address of the device.
     *
     * @return The IP address as a string, or an empty string if the command fails.
     */
    public String getIpAddress() {
        String dev = ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface;
        return NetworkUtils.getIPAddresses(dev);
    }

    /**
     * Returns a NetworkTraffic instance containing the average sent and recieved network traffic
     * since the last time this was called.
     *
     * @return NetworkTraffic instance with data in bits/second. The traffic values will be -1 if the
     *     data isn't available.
     */
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

    /**
     * Benchmarks SystemMonitor by timing the calls to retrieve metrics and writes the results to the
     * log.
     */
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
                        sb, () -> String.format("Usable Disk Space: %.0f MiB, ", getUsableDiskSpace() / mebi));
        total +=
                timeIt(
                        sb, () -> String.format("Memory: %.0f / %.0f MiB", getUsedMemory(), getTotalMemory()));
        total +=
                timeIt(
                        sb, () -> String.format("GPU Memory: %.0f / %.0f MiB", getGpuMemUtil(), getGpuMem()));
        total += timeIt(sb, () -> String.format("CPU Throttle: %s", getCpuThrottleReason()));

        total +=
                timeIt(
                        sb,
                        () -> {
                            var nt = getNetworkTraffic();
                            return String.format(
                                    "Data sent: %.0f Kbps, Data recieved: %.0f Kbps",
                                    nt.sentBitRate() / 1000, nt.recvBitRate() / 1000);
                        });

        sb.append(String.format("==========\n%7.3f ms\n", total));

        logger.info(sb.toString());
    }

    /**
     * Updates a StringBuilder with the result of calling `source` prepended by the time required to
     * run `source`, and returns the time (in ms) that a String Supplier takes. This can be used to
     * compare different ways of gathering the same metric.
     *
     * @param sb A StringBuilder used to collect the output from the supplier.
     * @param source A supplier that takes no arguments and returns a String.
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
