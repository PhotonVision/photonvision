package com.chameleonvision.vision.camera;

import com.chameleonvision.CameraException;
import com.chameleonvision.FileHelper;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.Pipeline;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import org.opencv.videoio.VideoCapture;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CameraManager {

	private static final Path CamConfigPath = Paths.get(SettingsManager.SettingsPath.toString(), "Cams");
	public static HashMap<String, Integer> CameraPorts = new HashMap<>();

	private static HashMap<String, Camera> AllCamerasByName = new HashMap<>();

	static HashMap<String, UsbCameraInfo> AllUsbCameraInfosByName = new HashMap<>() {{
		var suffix = 0;
		for (var info : UsbCamera.enumerateUsbCameras()) {
			var cap = new VideoCapture(info.dev);
			if (cap.isOpened()) {
				cap.release();
				var name = info.name;
				while (this.containsKey(name)) {
					suffix++;
					name = String.format("%s(%s)", info.name, suffix);
				}
				put(name, info);
			}
		}
	}};

	public static HashMap<String, Camera> getAllCamerasByName() {
		return AllCamerasByName;
	}

	public static boolean initializeCameras() {
		if (AllUsbCameraInfosByName.size() == 0) return false;
		FileHelper.CheckPath(CamConfigPath);
		for (var entry : AllUsbCameraInfosByName.entrySet()) {
			var camPath = Paths.get(CamConfigPath.toString(), String.format("%s.json", entry.getKey()));
			File camJsonFile = new File(camPath.toString());
			if (camJsonFile.exists() && camJsonFile.length() != 0) {
				try {
					Gson gson = new GsonBuilder().registerTypeAdapter(Camera.class, new CameraDeserializer()).create();
					var camJsonFileReader = new FileReader(camPath.toString());
					var gsonRead = gson.fromJson(camJsonFileReader, Camera.class);
					AllCamerasByName.put(entry.getKey(), gsonRead);
				} catch (FileNotFoundException ex) {
					ex.printStackTrace();
				}
			} else {
				if (!addCamera(new Camera(entry.getKey()), entry.getKey())) {
					System.err.println("Failed to add camera! Already exists!");
				}
			}
		}
		return true;
	}

	private static boolean addCamera(Camera camera, String cameraName) {
		if (AllCamerasByName.containsKey(cameraName)) return false;
		camera.addPipeline();
		AllCamerasByName.put(cameraName, camera);
		return true;
	}

	private static Camera getCamera(String cameraName) {
		return AllCamerasByName.get(cameraName);
	}

	public static Camera getCurrentCamera() throws CameraException {
		if (AllCamerasByName.size() == 0) throw new CameraException(CameraException.CameraExceptionType.NO_CAMERA);
		var curCam = AllCamerasByName.get(SettingsManager.GeneralSettings.curr_camera);
		if (curCam == null) throw new CameraException(CameraException.CameraExceptionType.BAD_CAMERA);
		return curCam;
	}

	public static void setCurrentCamera(String cameraName) throws CameraException {
		if (!AllCamerasByName.containsKey(cameraName))
			throw new CameraException(CameraException.CameraExceptionType.BAD_CAMERA);
		SettingsManager.GeneralSettings.curr_camera = cameraName;
		SettingsManager.updateCameraSetting(cameraName, getCurrentCamera().getCurrentPipelineIndex());
	}

	public static Pipeline getCurrentPipeline() throws CameraException {
		return getCurrentCamera().getCurrentPipeline();
	}

	public static void setCurrentPipeline(int pipelineNumber) throws CameraException {
		if (!getCurrentCamera().getPipelines().containsKey(pipelineNumber))
			throw new CameraException(CameraException.CameraExceptionType.BAD_PIPELINE);
		getCurrentCamera().setCurrentPipelineIndex(pipelineNumber);
		SettingsManager.updatePipelineSetting(pipelineNumber);
	}

	public static List<String> getResolutionList() throws CameraException {
		if (!SettingsManager.GeneralSettings.curr_camera.equals("")) {
			List<String> list = new ArrayList<>();
			for (var res : CameraManager.getCamera(SettingsManager.GeneralSettings.curr_camera).getAvailableVideoModes()) {
				list.add(String.format("%s X %s at %s fps", res.width, res.height, res.fps));
			}
			return list;
		}
		throw new CameraException(CameraException.CameraExceptionType.NO_CAMERA);
	}

	public static void saveCameras() {
		for (var entry : AllCamerasByName.entrySet()) {
			try {
				Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Camera.class, new CameraSerializer()).create();
				FileWriter writer = new FileWriter(Paths.get(CamConfigPath.toString(), String.format("%s.json", entry.getKey())).toString());
				gson.toJson(entry.getValue(), writer);
				writer.flush();
				writer.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
