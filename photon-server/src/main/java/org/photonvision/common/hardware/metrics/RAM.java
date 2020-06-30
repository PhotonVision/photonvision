package org.photonvision.common.hardware.metrics;

public class RAM extends MetricsBase {
    private static final String usageCommand = "sudo free  | awk -v i=2 -v j=3 'FNR == i {print $j}'";

    public static double getUsedRam() {
        return execute(usageCommand) / 1000;
    }
}
