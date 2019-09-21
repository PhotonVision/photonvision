package com.chameleonvision.settings;

import com.chameleonvision.FileHelper;
import com.chameleonvision.vision.GeneralSettings;
import com.chameleonvision.vision.camera.CameraManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class SettingsManager {
	public static final Path SettingsPath = Paths.get(System.getProperty("user.dir"), "Settings");
	public static com.chameleonvision.vision.GeneralSettings GeneralSettings;
	    public static HashMap<String, Integer> CameraPorts = new HashMap<>();

	private SettingsManager() {}

	public static void intialize() {
		initGeneralSettings();

		var allCameras = CameraManager.getAllCamerasByName();
		if (!allCameras.containsKey(GeneralSettings.curr_camera) && allCameras.size() > 0) {
			var cam = allCameras.entrySet().stream().findFirst().get().getValue();
			GeneralSettings.curr_camera = cam.name;
			GeneralSettings.curr_pipeline = cam.getCurrentPipelineIndex();
		}
	}

	private static void initGeneralSettings() {
		FileHelper.CheckPath(SettingsPath);
		try {
			GeneralSettings = new Gson().fromJson(new FileReader(Paths.get(SettingsPath.toString(), "Settings.json").toString()), com.chameleonvision.vision.GeneralSettings.class);
		} catch (FileNotFoundException e) {
			GeneralSettings = new GeneralSettings();
		}
	}

	public static void updateCameraSetting(String cameraName, int pipelineNumber) {
		GeneralSettings.curr_camera = cameraName;
		GeneralSettings.curr_pipeline = pipelineNumber;
	}

	public static void updatePipelineSetting(int pipelineNumber) {
		GeneralSettings.curr_pipeline = pipelineNumber;
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
