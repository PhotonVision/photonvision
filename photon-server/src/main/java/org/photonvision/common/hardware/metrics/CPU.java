package org.photonvision.common.hardware.metrics;

public class CPU extends MetricsBase {

    private CPU() {}

    public static CPU getInstance() {
        return new CPU();
    }

    private static final String memoryCommand = "sudo vcgencmd get_mem arm | grep -Eo '[0-9]+'";
    private static final String temperatureCommand =
            "sudo cat /sys/class/thermal/thermal_zone0/temp | grep -x -E '[0-9]+'";
    private static final String utilizationCommand =
            "sudo top -bn1 | grep \"Cpu(s)\" | sed \"s/.*, *\\([0-9.]*\\)%* id.*/\\1/\" | awk '{print 100 - $1}'";

    public double getMemory() {
        return execute(memoryCommand);
    }

    public double getTemp() {
        return execute(temperatureCommand) / 1000;
    }

    public double getUtilization() {
        return execute(utilizationCommand);
    }
}
