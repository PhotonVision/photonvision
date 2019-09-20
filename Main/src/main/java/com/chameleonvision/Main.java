package com.chameleonvision;

import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.camera.CameraManager;
import com.chameleonvision.vision.process.CameraProcess;
import com.chameleonvision.web.Server;

public class Main {
    public static void main(String[] args) {
        if (CameraManager.initializeCameras()) {
            SettingsManager manager = SettingsManager.getInstance();
            for (var camSet : CameraManager.getAllCamerasByName().entrySet()) {
                new Thread(new CameraProcess(camSet.getValue())).start();
            }
            //  NetworkTableInstance.getDefault().startClientTeam(SettingsManager.GeneralSettings.team_number);
            Server.main(8888);
        } else {
            System.err.println("No cameras connected!");
        }
    }
}
