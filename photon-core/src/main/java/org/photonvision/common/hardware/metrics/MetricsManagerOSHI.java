package org.photonvision.common.hardware.metrics;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.server.ExportException;
import java.util.Arrays;
import java.util.List;

import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;

import com.diozero.sbc.LocalSystemInfo;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.PhysicalProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import oshi.util.GlobalConfig;

public class MetricsManagerOSHI {
    private static MetricsManagerOSHI instance;

    private static Logger logger = new Logger(MetricsManagerOSHI.class, LogGroup.General);
    private static SystemInfo si;
    private static CentralProcessor cpu;
    private static OperatingSystem os;
    private static GlobalMemory mem;
    private static HardwareAbstractionLayer hal;
    private static FileStore fs;
    private static List<NetworkIF> iFaces;
    // private static LocalSystemInfo lsi;
    private long[] oldTicks; 
    private long lastUpdate = 0;
    
    private static final long MB = (1024*1024);

    private MetricsManagerOSHI() {
        logger.info("Starting MetricsManagerOSHI");
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LOADAVERAGE, true );

        si = new SystemInfo();
        hal = si.getHardware();
        os = si.getOperatingSystem();
        cpu = hal.getProcessor();
        mem = hal.getMemory();
        // lsi = LocalSystemInfo.getInstance();

        try {
            // get the filesystem for the directory photonvision is running on
            fs = Files.getFileStore(Path.of(""));
        } catch (IOException e) { 
            logger.error("Couldn't get FileStore for " + Path.of("")); 
            fs = null;
        }
        oldTicks = cpu.getSystemCpuLoadTicks();
        lastUpdate = System.currentTimeMillis();
        // lastTime = System.currentTimeMillis();
        iFaces = hal.getNetworkIFs();       
        
        TimedTaskManager.getInstance().addTask("Metrics", this::periodic, 5000);
    }

    public static MetricsManagerOSHI getInstance() {
        if (instance == null) {
            instance = new MetricsManagerOSHI();
        }
        return instance;
    }

    public void dumpMetricsToLog() {
        logger.info("Operating System: " + os.toString());
        logger.info("  System Uptime: " + FormatUtil.formatElapsedSecs(getUptime()));
        logger.info("  Elevated Privileges: " + os.isElevated() );

        var computerSystem = hal.getComputerSystem();
        logger.info("System: " + computerSystem.toString());
        logger.info("  Manufacturer: " + computerSystem.getManufacturer());
        logger.info("  Firmware: " + computerSystem.getFirmware());
        logger.info("  Baseboard: " + computerSystem.getBaseboard());
        logger.info("  Model: " + computerSystem.getModel());
        logger.info("  Serial Number: " + computerSystem.getSerialNumber());

        logger.info("CPU Info: " + cpu.toString());
        logger.info("  CPU Load: " + Arrays.toString(cpu.getSystemLoadAverage(3)));
        // logger.info(String.format("  CPU Utilization: %.2f%%", getCpuUsage()));
        logger.info("  Max Frequency: " + FormatUtil.formatHertz(cpu.getMaxFreq()));
        StringBuilder frequencies = new StringBuilder();
        for (long freq : cpu.getCurrentFreq()) {
            frequencies.append(FormatUtil.formatHertz(freq));
            frequencies.append(", ");
        }
        logger.info("  Current Frequency: " + frequencies);
        for ( PhysicalProcessor core : cpu.getPhysicalProcessors() ) {
            logger.info("  Core " + core.getPhysicalProcessorNumber() + " (" + core.getEfficiency() + ")");
        }

        logger.info("Physical Memory: " + mem.toString());
        logger.info("  Total: " + mem.getTotal());
        logger.info("  Available: " + mem.getAvailable());

        logger.info(String.format("Memory: %.0f / %.0f MB", getUsedMemory(), getTotalMemory()));

        var myProc = os.getCurrentProcess();
        logger.info( "Current Process: " + myProc.getName() + ", PID: " + myProc.getProcessID());
        // logger.info("  Command Line: " + myProc.getCommandLine());
        logger.info("  Kernel Time: " + myProc.getKernelTime());
        logger.info("  User Time: " + myProc.getUserTime());
        logger.info("  Cumulative Load: " + myProc.getProcessCpuLoadCumulative());
        logger.info("  Up Time: " + myProc.getUpTime());
        logger.info("  Priority: " + myProc.getPriority());
        logger.info("  User: " + myProc.getUser());
        logger.info("  Threads: " + myProc.getThreadCount());

        if (Files.exists(Path.of("/proc/device-tree/model"))) {
            logger.info("CPU Temperature (diozero): " + LocalSystemInfo.getInstance().getCpuTemperature());
        } else {
            logger.info("diozero Cannot read CPU Temperature");
        }
        logger.info(String.format("CPU Temperature (oshi): %.2f °C", getCpuTemperature()));

        logger.info(String.format( "Used Disk: %.2f%%", getUsedDiskPct()));
        
        logger.info("Network Interfaces");
        for (NetworkIF iFace : hal.getNetworkIFs() ) {
            logger.info("  Interface: " + iFace.toString());
        }

        logger.info("Graphics Cards");
        for ( GraphicsCard gc : hal.getGraphicsCards()) {
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
            usedPct = 100.0*(1.0 - fs.getUsableSpace()/total);
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
        logger.info("  oldTicks: " + Arrays.toString(oldTicks));
        logger.info("  newTicks: " + Arrays.toString(newTicks));
        var cpuLoad = cpu.getSystemCpuLoadBetweenTicks(oldTicks, newTicks);
        oldTicks = newTicks;
        return 100.0 * cpuLoad;
    }

    public record NetworkTraffic(double sent, double recv) { }

    public NetworkTraffic getNetworkTraffic() {
        long sent = 0;
        long recv = 0;
        logger.info("Managed Interface: " + ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface);
        for (var iFace : iFaces ) {
            logger.info("Network Interface: " + iFace.getName());
            // if (iFace.getName().equals(ConfigManager.getInstance().getConfig().getNetworkConfig().networkManagerIface)) {
                var lastBytesSent = iFace.getBytesSent();
                var lastBytesRecv = iFace.getBytesRecv();
                logger.info("  lastBytesSent: " + lastBytesSent + "| lastBytesRecv: " + lastBytesRecv);
                iFace.updateAttributes();
                sent = iFace.getBytesSent() - lastBytesSent;
                recv = iFace.getBytesRecv() - lastBytesRecv;
                logger.info("  Sent: " + sent + "| Recv: " + recv);
                // break;
            // }
        }
        long now = System.currentTimeMillis();
        double dTime = (now - lastUpdate)/1000.0;
        if (dTime > 0) {
            lastUpdate = now;
            return new NetworkTraffic(sent/dTime, recv/dTime);
        }
        return new NetworkTraffic(0, 0);
    }

    public void periodic() {
        logger.info("CPU Load: " + Arrays.toString(cpu.getSystemLoadAverage(3)));
        logger.info(String.format("CPU Usage: %.2f%%", getCpuUsage()));
        logger.info(String.format("CPU Temperature: %.2f °C", getCpuTemperature()));
        logger.info(String.format( "Used Disk: %.2f%%", getUsedDiskPct()));
        logger.info(String.format("Memory: %.0f / %.0f MB", getUsedMemory(), getTotalMemory()));
        var nt = getNetworkTraffic();
        logger.info(String.format("Data sent: %.2f B/s, Data recieved: %.2f B/s", nt.sent(), nt.recv()));
    }
}
