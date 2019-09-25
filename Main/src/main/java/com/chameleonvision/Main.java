package com.chameleonvision;

import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.camera.CameraManager;
import com.chameleonvision.vision.process.VisionProcess;
import com.chameleonvision.web.Server;
import edu.wpi.first.networktables.NetworkTableInstance;

public class Main {
	public static void main(String[] args) {
		if (CameraManager.initializeCameras()) {
			SettingsManager.initialize();
			for (var camSet : CameraManager.getAllCamerasByName().entrySet()) {
				new Thread(new VisionProcess(camSet.getValue())).start();
			}
			NetworkTableInstance.getDefault().startClient("localhost");

			//  NetworkTableInstance.getDefault().startClientTeam(SettingsManager.GeneralSettings.team_number);
			Server.main(8888);
		} else {
			System.err.println("No cameras connected!");
		}
	}
}
