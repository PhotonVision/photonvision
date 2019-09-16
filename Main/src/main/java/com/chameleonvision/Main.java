package com.chameleonvision;

import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.process.CameraProcess;
import com.chameleonvision.web.Server;
import edu.wpi.cscore.UsbCamera;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        SettingsManager manager = SettingsManager.getInstance();
//        NetworkTableInstance.getDefault().startClientTeam(SettingsManager.GeneralSettings.team_number);
        for (Map.Entry<String, UsbCamera> entry : SettingsManager.UsbCameras.entrySet()) {
            new Thread(new CameraProcess(entry.getKey())).start();
        }
        Server.main(8888);
    }
}
