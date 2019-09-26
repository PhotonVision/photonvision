package com.chameleonvision;

import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.camera.CameraManager;
import com.chameleonvision.vision.process.VisionProcess;
import com.chameleonvision.web.Server;
import edu.wpi.cscore.CameraServerCvJNI;
import edu.wpi.cscore.CameraServerJNI;
import edu.wpi.first.networktables.NetworkTableInstance;

import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		// Attempt to load the JNI Libraries
		try {
			CameraServerJNI.forceLoad();
			CameraServerCvJNI.forceLoad();
		} catch (IOException e) {
			var errorStr = SettingsManager.getCurrentPlatform().equals(SettingsManager.Platform.UNSUPPORTED) ? "Unsupported platform!" : "Failed to load JNI Libraries!";
			throw new RuntimeException(errorStr);
		}

		if (CameraManager.initializeCameras()) {
			SettingsManager.initialize();
			for (var camSet : CameraManager.getAllCamerasByName().entrySet()) {
				new Thread(new VisionProcess(camSet.getValue())).start();
			}

			NetworkTableInstance.getDefault().startClientTeam(SettingsManager.GeneralSettings.team_number);
//			NetworkTableInstance.getDefault().startClient("localhost");
			Server.main(8888);
		} else {
			System.err.println("No cameras connected!");
		}
	}
}
