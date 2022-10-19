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

public class CPUMetrics extends MetricsBase {
    private String cpuMemSplit = null;

    public String getMemory() {
        if (cpuMemoryCommand.isEmpty()) return "";
        if (cpuMemSplit == null) {
            cpuMemSplit = execute(cpuMemoryCommand);
        }
        return cpuMemSplit;
    }

    public String getTemp() {
        if (cpuTemperatureCommand.isEmpty()) return "";
        try {
            return execute(cpuTemperatureCommand);
        } catch (Exception e) {
            return "N/A";
        }
    }

    public String getUtilization() {
        return execute(cpuUtilizationCommand);
    }

    public String getUptime() {
        return execute(cpuUptimeCommand);
    }

    private int getThrottlingFlags(){
        if(cpuThrottlingCommand.isEmpty()) return 0;
        int throttleFlags = 0;
        try {
            String throttleFlagsStr = execute(cpuThrottlingCommand).trim();
            logger.debug("Read throttling flags: " + throttleFlagsStr);
            throttleFlags = Integer.decode(throttleFlagsStr);
        } catch (Exception e) {
            logger.warn("Could not run throttling flags read command " + cpuThrottlingCommand);
            logger.warn(e.toString());
        }
        return throttleFlags;
    }

    public String getActiveThrottling(){
        int throttleFlags = getThrottlingFlags();

        //Note these are specific to raspberry PI & ARM
        //from https://pimylifeup.com/raspberry-pi-low-voltage-warning/
        String retStr = "";
        if((throttleFlags & 0x1) != 0){ retStr += " LV"; }
        if((throttleFlags & 0x2) != 0){ retStr += " FRQ"; }
        if((throttleFlags & 0x4) != 0){ retStr += " THR"; }
        if((throttleFlags & 0x8) != 0){ retStr += " TEMP"; }

        if(retStr.length() == 0){
            retStr = " None";
        }

        return retStr;
    }

    public String getPrevThrottling(){
        int throttleFlags = getThrottlingFlags();

        //Note these are specific to raspberry PI & ARM
        //from https://pimylifeup.com/raspberry-pi-low-voltage-warning/
        String retStr = "";
        if((throttleFlags & 0x10000) != 0){ retStr += " LV"; }
        if((throttleFlags & 0x20000) != 0){ retStr += " FRQ"; }
        if((throttleFlags & 0x40000) != 0){ retStr += " THR"; }
        if((throttleFlags & 0x80000) != 0){ retStr += " TEMP"; }

        if(retStr.length() == 0){
            retStr = " None";
        }

        return retStr;
    }
}
