package Classes;
import Objects.*;
import java.io.*;
import java.nio.file.*;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.wpi.cscore.*;
import org.opencv.videoio.VideoCapture;

public class SettingsManager  {
    private static SettingsManager instance;
    private SettingsManager() {
        InitiateGeneralSettings();
        InitiateCamerasInfo();
        InitiateUsbCameras();
        InitiateUsbCamerasSettings();
    }
    public static synchronized SettingsManager getInstance(){
        if(instance == null){
            synchronized (SettingsManager.class) {
                if(instance == null){
                    instance = new SettingsManager();
                }
            }
        }
        return instance;
    }
    public static HashMap cams = new HashMap();
    public static HashMap UsbCameras = new HashMap();
    public static HashMap<String,UsbCameraInfo> USBCamerasInfo = new HashMap<String,UsbCameraInfo>();
    public static DefaultGeneralSettings GeneralSettings;
    public static HashMap CameraPort = new HashMap();
    public static HashMap CamerasCurrentPipeline = new HashMap();
    private Path SettingsPath = Paths.get(System.getProperty("user.dir"),"Settings");
    private Path CamsPath = Paths.get(SettingsPath.toString(),"Cams");


    private void InitiateGeneralSettings(){
        if (!Files.exists(SettingsPath)){
            try {
                Files.createDirectories(SettingsPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            GeneralSettings = new Gson().fromJson(new FileReader(Paths.get(SettingsPath.toString(),"Settings.json").toString()),DefaultGeneralSettings.class);
        } catch (FileNotFoundException e) {
            GeneralSettings = new DefaultGeneralSettings();
        }
    }

    private void InitiateCamerasInfo(){
        List<Integer> TrueCameras = new ArrayList<Integer>();
        UsbCameraInfo[] UsbDevices = UsbCamera.enumerateUsbCameras();
        for (var i=0; i < UsbDevices.length; i++){
            var cap = new VideoCapture(UsbDevices[i].dev);
            if (cap.isOpened()){
                TrueCameras.add(i);
                cap.release();
            }

        }
        for (var i: TrueCameras){
            var DeviceName = UsbDevices[i].name;
            var suffix = 0;
            while (USBCamerasInfo.containsKey(DeviceName)){
                suffix++;
                DeviceName = String.format("%s(%s)",UsbDevices[i].name,suffix);
            }
            USBCamerasInfo.put(DeviceName,UsbDevices[i]);
        }
    }
    private void InitiateUsbCameras(){

    }
    private void InitiateUsbCamerasSettings(){

    }
}
