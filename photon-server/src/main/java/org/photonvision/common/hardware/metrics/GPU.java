package org.photonvision.common.hardware.metrics;


public class GPU extends MetricsBase {
    private static final String memoryCommand = "sudo vcgencmd get_mem gpu | grep -Eo '[0-9]+'";
    private static final String temperatureCommand =
            "sudo vcgencmd measure_temp | sed 's/[^0-9]*//g'\n";

    public static double getMemory() {
        return execute(memoryCommand);
    }

    public static double getTemp() {
        return execute(temperatureCommand) / 10;
    }
}
