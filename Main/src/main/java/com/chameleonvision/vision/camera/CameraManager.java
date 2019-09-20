package com.chameleonvision.vision.camera;

import com.chameleonvision.FileHelper;
import com.chameleonvision.CameraException;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.vision.Pipeline;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.UsbCameraInfo;
import org.opencv.videoio.VideoCapture;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CameraManager {

    private static final Path CamConfigPath = Paths.get(SettingsManager.SettingsPath.toString(), "Cams");

    // TODO: Fix suffix for camera
    // TODO: throw a camera Exception if no camera is connected
    static HashMap<String, UsbCameraInfo> AllUsbCameraInfosByName = new HashMap<>() {{
        var suffix = 0;
        for (var info : UsbCamera.enumerateUsbCameras()) {
            var cap = new VideoCapture(info.name);
            if (cap.isOpened()) {
                cap.release();
            }
            var name = info.name;
            while (this.containsKey(name)) {
                suffix++;
                name = String.format("%s(%s)", info.name, suffix);
            }
            put(name, info);
        }
    }};

    private static HashMap<String, Camera> AllCamerasByName = new HashMap<>();

    private static String currentCameraName;

    public static HashMap<String, Camera> getAllCamerasByName() { return AllCamerasByName; }

    public static void initializeCameras() {
        FileHelper.CheckPath(CamConfigPath);
        for (var entry : AllUsbCameraInfosByName.entrySet()) {
            var camPath = Paths.get(CamConfigPath.toString(), String.format("%s.json", entry.getKey()));
            if (Files.exists(camPath)) {
                try {
                    // TODO: Check if deserializing correctly, if not, add CameraDeserializer
                    var camJsonFile = new FileReader(camPath.toString());
                    var gsonRead = new Gson().fromJson(camJsonFile, Camera.class);
                    AllCamerasByName.put(entry.getKey(), gsonRead);
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                }
            } else {
                if (!addCamera(new Camera(entry.getKey()))) {
                    System.err.println("Failed to add camera! Already exists!");
                }
            }
            // TODO: Set currentCameraName from GeneralSettings instead of this
            if (currentCameraName == null && AllCamerasByName.size() == 1) { // set current camera to first found
                currentCameraName = AllCamerasByName.keySet().stream().findFirst().get();
            }
        }
    }

    private static boolean addCamera(Camera camera) {
        if (AllCamerasByName.containsKey(camera.name)) return false;
        camera.addPipeline();
        AllCamerasByName.put(camera.name, camera);
        return true;
    }

    private static Camera getCamera(String cameraName) {
        return AllCamerasByName.get(cameraName);
    }

    public static void setCurrentCamera(String cameraName) throws CameraException {
        if (!AllCamerasByName.containsKey(cameraName)) throw new CameraException(CameraException.CameraExceptionType.BAD_CAMERA);
        currentCameraName = cameraName;
        SettingsManager.getInstance().updateCameraSetting(cameraName, getCurrentCamera().getCurrentPipelineIndex());
    }

    public static Camera getCurrentCamera() throws CameraException {
        if (AllCamerasByName.size() == 0) throw new CameraException(CameraException.CameraExceptionType.NO_CAMERA);
        var curCam = AllCamerasByName.get(currentCameraName);
        if (curCam == null) throw new CameraException(CameraException.CameraExceptionType.BAD_CAMERA);
        return curCam;
    }

    public static void setCurrentPipeline(int pipelineNumber) throws CameraException {
        if (!getCurrentCamera().getPipelines().containsKey(pipelineNumber)) throw new CameraException(CameraException.CameraExceptionType.BAD_PIPELINE);
        getCurrentCamera().setCurrentPipelineIndex(pipelineNumber);
        SettingsManager.getInstance().updatePipelineSetting(pipelineNumber);
    }

    public static Pipeline getCurrentPipeline() throws CameraException {
        return getCurrentCamera().getCurrentPipeline();
    }

    public static List<String> getResolutionList() throws CameraException {
        if (!currentCameraName.equals("")) {
            List<String> list = new ArrayList<>();
            var cam = CameraManager.getCamera(currentCameraName).UsbCam;
            for (var res : cam.enumerateVideoModes()) {
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
