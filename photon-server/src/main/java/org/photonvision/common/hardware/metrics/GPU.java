package org.photonvision.common.hardware.metrics;

public class GPU extends MetricsBase {

    private GPU() {}

    public static GPU getInstance() {
        return new GPU();
    }

    private static final String memoryCommand = "sudo vcgencmd get_mem gpu | grep -Eo '[0-9]+'";
    private static final String temperatureCommand =
            "sudo vcgencmd measure_temp | sed 's/[^0-9]*//g'\n";

    public double getMemory() {
        return execute(memoryCommand);
    }

    public double getTemp() {
        return execute(temperatureCommand) / 10;
    }
}
