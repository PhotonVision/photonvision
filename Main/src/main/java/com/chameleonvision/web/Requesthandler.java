package com.chameleonvision.web;

import com.chameleonvision.network.NetworkIPMode;
import com.chameleonvision.settings.GeneralSettings;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.camera.CameraException;
import com.chameleonvision.vision.camera.CameraManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;

public class Requesthandler {

    public static void onGeneralSettings(Context ctx) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map map = objectMapper.readValue(ctx.body(), Map.class);
            SettingsManager.GeneralSettings.teamNumber = (int) map.get("teamNumber");
            SettingsManager.GeneralSettings.connectionType = NetworkIPMode.values()[(int) map.get("connectionType")];
            SettingsManager.GeneralSettings.ip = (String) map.get("ip");
            SettingsManager.GeneralSettings.netmask = (String) map.get("netmask");
            SettingsManager.GeneralSettings.gateway = (String) map.get("gateway");
            SettingsManager.GeneralSettings.hostname = (String) map.get("hostname");
            SettingsManager.saveSettings();
            SocketHandler.sendFullSettings();
            ctx.status(200);
        } catch (JsonProcessingException e) {
            ctx.status(500);
        }
    }

    public static void onCameraSettings(Context ctx) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map camSettings = objectMapper.readValue(ctx.body(), Map.class);
            var curCam = CameraManager.getCurrentCamera();

            Number newFOV = (Number) camSettings.get("fov");
            Integer newStreamDivisor = (Integer) camSettings.get("streamDivisor");
            Integer newResolution = (Integer) camSettings.get("resolution");

            curCam.setFOV(newFOV);

            var currentStreamDivisorOrdinal = curCam.getStreamDivisor().ordinal();
            if (currentStreamDivisorOrdinal != newStreamDivisor) {
                curCam.setStreamDivisor(newStreamDivisor, true);
            }

            var currentResolutionIndex = curCam.getVideoModeIndex();
            if (currentResolutionIndex != newResolution) {
                curCam.setCamVideoMode(newResolution, true);
            }

            CameraManager.saveCameras();
            SocketHandler.sendFullSettings();
            ctx.status(200);
        } catch (JsonProcessingException | CameraException e) {
            e.printStackTrace();
            ctx.status(500);
        }
    }
}
