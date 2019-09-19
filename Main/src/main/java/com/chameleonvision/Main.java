package com.chameleonvision;

import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.camera.Camera;
import com.chameleonvision.vision.camera.CameraManager;
import com.chameleonvision.vision.process.CameraProcess;
import com.chameleonvision.web.Server;
import edu.wpi.cscore.UsbCamera;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        CameraManager.initializeCameras();
        SettingsManager manager = SettingsManager.getInstance();
        for (var camSet : CameraManager.getAllCamerasByName().entrySet()) {
            new Thread(new CameraProcess(camSet.getValue())).start();
        }
        //  NetworkTableInstance.getDefault().startClientTeam(SettingsManager.GeneralSettings.team_number);
        Server.main(8888);
    }
}
