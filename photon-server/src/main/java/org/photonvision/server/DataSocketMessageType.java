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

package org.photonvision.server;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public enum DataSocketMessageType {
    SMT_DRIVERMODE("driverMode"),
    SMT_CHANGECAMERANAME("changeCameraName"),
    SMT_CHANGEPIPELINENAME("changePipelineName"),
    SMT_ADDNEWPIPELINE("addNewPipeline"),
    SMT_DELETECURRENTPIPELINE("deleteCurrentPipeline"),
    SMT_CURRENTCAMERA("currentCamera"),
    SMT_PIPELINESETTINGCHANGE("changePipelineSetting"),
    SMT_CURRENTPIPELINE("currentPipeline"),
    SMT_STARTPNPCALIBRATION("startPnpCalibration"),
    SMT_TAKECALIBRATIONSNAPSHOT("takeCalibrationSnapshot"),
    SMT_DUPLICATEPIPELINE("duplicatePipeline"),
    SMT_CHANGEBRIGHTNESS("enabledLEDPercentage"),
    SMT_ROBOTOFFSETPOINT("robotOffsetPoint"),
    SMT_CHANGEPIPELINETYPE("pipelineType");

    public final String entryKey;

    DataSocketMessageType(String entryKey) {
        this.entryKey = entryKey;
    }

    private static final Map<String, DataSocketMessageType> entryKeyToValueMap = new HashMap<>();

    static {
        for (var value : EnumSet.allOf(DataSocketMessageType.class)) {
            entryKeyToValueMap.put(value.entryKey, value);
        }
    }

    public static DataSocketMessageType fromEntryKey(String entryKey) {
        return entryKeyToValueMap.get(entryKey);
    }
}
