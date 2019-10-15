package com.chameleonvision.settings;

import com.chameleonvision.network.NetworkManager;
import com.chameleonvision.util.FileHelper;
import com.chameleonvision.vision.camera.CameraManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsManager {
	public static final Path SettingsPath = Paths.get(System.getProperty("user.dir"), "settings");
	public static com.chameleonvision.settings.GeneralSettings GeneralSettings;

	private SettingsManager() {}

	public static void initialize() {
		initGeneralSettings();
//		if (manageNetwork) {

//            NetworkSettings netSettings = new NetworkSettings();
//            netSettings.hostname = GeneralSettings.hostname;
//            netSettings.gateway = GeneralSettings.gateway;
//            netSettings.netmask = GeneralSettings.netmask;
//            netSettings.connectionType = GeneralSettings.connection_type;
//            netSettings.ip = GeneralSettings.ip;
//            netSettings.run();
//        }
		var allCameras = CameraManager.getAllCamerasByName();
		if (!allCameras.containsKey(GeneralSettings.currentCamera) && allCameras.size() > 0) {
			var cam = allCameras.entrySet().stream().findFirst().get().getValue();
			GeneralSettings.currentCamera = cam.name;
			GeneralSettings.currentPipeline = cam.getCurrentPipelineIndex();
		}
	}

	private static void initGeneralSettings() {
		FileHelper.CheckPath(SettingsPath);
		try {
			GeneralSettings = new Gson().fromJson(new FileReader(Paths.get(SettingsPath.toString(), "settings.json").toString()), com.chameleonvision.settings.GeneralSettings.class);
		} catch (FileNotFoundException e) {
			GeneralSettings = new GeneralSettings();
		}
	}

	public static void updateCameraSetting(String cameraName, int pipelineNumber) {
		GeneralSettings.currentCamera = cameraName;
		GeneralSettings.currentPipeline = pipelineNumber;
	}

	public static void updatePipelineSetting(int pipelineNumber) {
		GeneralSettings.currentPipeline = pipelineNumber;
	}

	public static void saveSettings() {
		CameraManager.saveCameras();
		saveGeneralSettings();
	}

	private static void saveGeneralSettings() {
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			FileWriter writer = new FileWriter(Paths.get(SettingsPath.toString(), "settings.json").toString());
			gson.toJson(GeneralSettings, writer);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
