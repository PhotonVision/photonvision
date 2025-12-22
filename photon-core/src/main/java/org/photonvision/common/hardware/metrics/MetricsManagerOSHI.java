package org.photonvision.common.hardware.metrics;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

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
    // private static LocalSystemInfo lsi;
    // private long[] oldTicks; 
    // private long lastTime = 0;
    // private List<NetworkIF> iFaces;
    
    private MetricsManagerOSHI() {
        logger.info("Starting MetricsManagerOSHI");
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LOADAVERAGE, "true" );
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
        // oldTicks = new long[TickType.values().length];
        // lastTime = System.currentTimeMillis();
        // iFaces = hal.getNetworkIFs();        
    }

    public static MetricsManagerOSHI getInstance() {
        if (instance == null) {
            instance = new MetricsManagerOSHI();
        }
        return instance;
    }

    public void dumpMetricsToLog() {
        logger.info("Operating System: " + os.toString());
        logger.info("  System Uptime: " + FormatUtil.formatElapsedSecs(os.getSystemUptime()));
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
        logger.info("  CPU Temperature: " + LocalSystemInfo.getInstance().getCpuTemperature());
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

        logger.info("Sensors: " + hal.getSensors().toString());

        logger.info("Used Disk (pct): " + getUsedDiskPct());
        
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

}
