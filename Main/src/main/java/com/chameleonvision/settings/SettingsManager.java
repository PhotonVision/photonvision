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

public class SettingsManager {
	public static final Path SettingsPath = Paths.get(System.getProperty("user.dir"), "Settings");
	public static com.chameleonvision.vision.GeneralSettings GeneralSettings;

	private SettingsManager() {}

	public static void initialize() {
		initGeneralSettings();
		NetworkSettings netSettings = new NetworkSettings();
		netSettings.hostname = GeneralSettings.hostname;
		netSettings.gateway = GeneralSettings.gateway;
		netSettings.netmask = GeneralSettings.netmask;
		netSettings.connectionType = GeneralSettings.connection_type;
		netSettings.ip = GeneralSettings.ip;
		netSettings.run();

		var allCameras = CameraManager.getAllCamerasByName();
		if (!allCameras.containsKey(GeneralSettings.curr_camera) && allCameras.size() > 0) {
			var cam = allCameras.entrySet().stream().findFirst().get().getValue();
			GeneralSettings.curr_camera = cam.name;
			GeneralSettings.curr_pipeline = cam.getCurrentPipelineIndex();
		}
	}

	public enum Platform {
		WINDOWS_64("Windows x64"),
		LINUX_64("Linux x64"),
		LINUX_RASPBIAN("Linux Raspbian"),
		LINUX_AARCH64("Linux ARM 64bit"),
		MACOS_64("Mac OS x64"),
		UNSUPPORTED("Unsupported Platform");

		public final String value;

		Platform(String value) {
			this.value = value;
		}
	}

	public static Platform getCurrentPlatform() {
		var osName = System.getProperty("os.name");
		var osArch = System.getProperty("os.arch");

		if (osName.contains("Windows")) {
			if (osArch.equals("amd64")) return Platform.WINDOWS_64;
			return Platform.UNSUPPORTED;
		}

		if (osName.contains("Linux")) {
			if (osArch.equals("amd64")) return Platform.LINUX_64;
			if (osArch.contains("rasp")) return Platform.LINUX_RASPBIAN;
			if (osArch.contains("aarch")) return Platform.LINUX_64;
			return Platform.UNSUPPORTED;
		}

		if (osName.contains("Mac")) {
			if (osArch.equals("amd64")) return Platform.MACOS_64;
			return Platform.UNSUPPORTED;
		}

		return Platform.UNSUPPORTED;
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
