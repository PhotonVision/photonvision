package com.chameleonvision.vision.camera;

import com.chameleonvision.util.FileHelper;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.Pipeline;
import com.chameleonvision.vision.process.USBCameraProcess;
import com.chameleonvision.vision.process.VisionProcess;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import org.opencv.videoio.VideoCapture;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CameraManager {

	private static final Path CamConfigPath = Paths.get(SettingsManager.SettingsPath.toString(), "cameras");

	private static LinkedHashMap<String, USBCamera> allCamerasByName = new LinkedHashMap<>();
	public static HashMap<String, VisionProcess> allVisionProcessesByName = new HashMap<>();

	static HashMap<String, UsbCameraInfo> allUsbCameraInfosByName = new HashMap<>() {{
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

	public static HashMap<String, USBCamera> getAllCamerasByName() {
		return allCamerasByName;
	}
	public static List<String> getAllCameraByNickname() {
		var cameras = getAllCamerasByName();
		return cameras.values().stream().map(USBCamera::getNickname).collect(Collectors.toList());
	}

	public static boolean initializeCameras() {
		if (allUsbCameraInfosByName.size() == 0) return false;
		FileHelper.CheckPath(CamConfigPath);
		allUsbCameraInfosByName.forEach((key, value) -> {
			var camPath = Paths.get(CamConfigPath.toString(), String.format("%s.json", key));
			File camJsonFile = new File(camPath.toString());
			if (camJsonFile.exists() && camJsonFile.length() != 0) {
				try {
					Gson gson = new GsonBuilder().registerTypeAdapter(USBCamera.class, new CameraDeserializer()).create();
					var camJsonFileReader = new FileReader(camPath.toString());
					var gsonRead = gson.fromJson(camJsonFileReader, USBCamera.class);
					allCamerasByName.put(key, gsonRead);
				} catch (FileNotFoundException ex) {
					ex.printStackTrace();
				}
			} else {
				if (!addCamera(new USBCamera(key), key)) {
					System.err.println("Failed to add camera! Already exists!");
				}
			}
		});
		return true;
	}

	public static void initializeThreads(){
		allCamerasByName.forEach((name, camera) -> {
			VisionProcess visionProcess = new VisionProcess(new USBCameraProcess(camera));
			allVisionProcessesByName.put(name, visionProcess);
			new Thread(visionProcess).start();
		});
	}

	private static boolean addCamera(USBCamera USBCamera, String cameraName) {
		if (allCamerasByName.containsKey(cameraName)) return false;
		USBCamera.addPipeline();
		allCamerasByName.put(cameraName, USBCamera);
		return true;
	}

	private static USBCamera getCamera(String cameraName) {
		return allCamerasByName.get(cameraName);
	}

	public static USBCamera getCameraByIndex(int index) {
		return allCamerasByName.get( (allCamerasByName.keySet().toArray())[ index ] );
	}

	public static USBCamera getCurrentCamera() throws CameraException {
		if (allCamerasByName.size() == 0) throw new CameraException(CameraException.CameraExceptionType.NO_CAMERA);
		var curCam = allCamerasByName.get(SettingsManager.generalSettings.currentCamera);
		if (curCam == null) throw new CameraException(CameraException.CameraExceptionType.BAD_CAMERA);
		return curCam;
	}

	public static Integer getCurrentCameraIndex() throws CameraException {
		if (allCamerasByName.size() == 0) throw new CameraException(CameraException.CameraExceptionType.NO_CAMERA);
		List<String> arr = new ArrayList<>(allCamerasByName.keySet());
		for (var i = 0; i < allCamerasByName.size(); i++){
			if (SettingsManager.generalSettings.currentCamera.equals(arr.get(i))){
				return i;
			}
		}
		return null;
	}

	public static void setCurrentCamera(String cameraName) throws CameraException {
		if (!allCamerasByName.containsKey(cameraName))
			throw new CameraException(CameraException.CameraExceptionType.BAD_CAMERA);
		SettingsManager.generalSettings.currentCamera = cameraName;
		SettingsManager.updateCameraSetting(cameraName, getCurrentCamera().getCurrentPipelineIndex());
	}

	public static void setCurrentCamera(int cameraIndex) throws CameraException {
		List<String> s =   new ArrayList<String>(allCamerasByName.keySet());
		setCurrentCamera(s.get(cameraIndex));
	}

	public static Pipeline getCurrentPipeline() throws CameraException {
		return getCurrentCamera().getCurrentPipeline();
	}

	public static void setCurrentPipeline(int pipelineNumber) throws CameraException {
		if (pipelineNumber >= getCurrentCamera().getPipelines().size()){
			throw new CameraException(CameraException.CameraExceptionType.BAD_PIPELINE);
		}
		getCurrentCamera().setCurrentPipelineIndex(pipelineNumber);
		SettingsManager.updatePipelineSetting(pipelineNumber);
	}

	public static VisionProcess getVisionProcessByCameraName(String cameraName)	{
		return allVisionProcessesByName.get(cameraName);
	}

	public static VisionProcess getCurrentVisionProcess() throws CameraException {
		if (!SettingsManager.generalSettings.currentCamera.equals("")){
			return allVisionProcessesByName.get(SettingsManager.generalSettings.currentCamera);
		}
		throw new CameraException(CameraException.CameraExceptionType.NO_CAMERA);
	}

	public static void saveCameras() {
		for (var entry : allCamerasByName.entrySet()) {
			try {
				Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(USBCamera.class, new CameraSerializer()).create();
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
