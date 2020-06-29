package org.photonvision.server;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public enum SocketMessageType {
    SMT_DRIVERMODE("driverMode"),
    SMT_CHANGECAMERANAME("changeCameraName"),
    SMT_CHANGEPIPELINENAME("changePipelineName"),
    SMT_ADDNEWPIPELINE("addNewPipeline"),
    SMT_COMMAND("command"),
    SMT_CURRENTCAMERA("currentCamera"),
    SMT_PIPELINESETTINGCHANGE("changePipelineSetting"),
    SMT_CURRENTPIPELINE("currentPipeline"),
    SMT_ISPNPCALIBRATION("isPNPCalibration"),
    SMT_TAKECALIBRATIONSNAPSHOT("takeCalibrationSnapshot");

    public final String entryKey;

    SocketMessageType(String entryKey) {
        this.entryKey = entryKey;
    }

    private static final Map<String, SocketMessageType> entryKeyToValueMap = new HashMap<>();

    static {
        for (var value : EnumSet.allOf(SocketMessageType.class)) {
            entryKeyToValueMap.put(value.entryKey, value);
        }
    }

    public static SocketMessageType fromEntryKey(String entryKey) {
        return entryKeyToValueMap.get(entryKey);
    }
}