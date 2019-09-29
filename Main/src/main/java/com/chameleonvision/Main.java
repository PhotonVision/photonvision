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
	private static final String PORT_KEY = "-port";
	public static void main(String[] args) {
		int port = 8888;
		for(int i=0; i<args.length; i+=2)
		{
			String key = args[i];
			String value = args[i+1];
			if (PORT_KEY.equals(key)) {
				try{
					port = Integer.parseInt(value);
				}
				catch (NumberFormatException e){
					System.err.println("Given Port Was Not A Number Starting Server At Default Port");
				}
			}
		}
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
			CameraManager.initializeThreads();
			NetworkTableInstance.getDefault().startClientTeam(SettingsManager.GeneralSettings.team_number);
//			NetworkTableInstance.getDefault().startClient("localhost");
			System.out.println("Starting WebServer At Port:" + port);
			Server.main(port);
		} else {
			System.err.println("No cameras connected!");
		}
	}
}
