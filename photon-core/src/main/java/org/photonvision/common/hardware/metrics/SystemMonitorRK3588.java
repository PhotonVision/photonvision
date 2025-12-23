package org.photonvision.common.hardware.metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemMonitorRK3588 extends SystemMonitor {

    final String regex = "Core\\d:\\s*(\\d+)%";
    final Pattern pattern = Pattern.compile(regex);

    @Override
    public double[] getNpuUsage() {
        try {
            var contents = Files.readString(Path.of("/sys/kernel/debug/rknpu/load"));
            Matcher matcher = pattern.matcher(contents);
            double[] results =
                    matcher.results().map(mr -> mr.group(1)).mapToDouble(Double::parseDouble).toArray();
            return results;
        } catch (IOException e) {
            return new double[] {-1};
        }
    }
}
