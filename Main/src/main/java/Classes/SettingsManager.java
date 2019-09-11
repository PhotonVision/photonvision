package Classes;
import Objects.*;
import org.json.JSONObject;

import java.util.HashMap;

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
    public static HashMap USBCamerasInfo = new HashMap();
    public static DefaultGeneralSettings GeneralSettings;
    public static HashMap CameraPort = new HashMap();
    public static HashMap CamerasCurrentPipeline = new HashMap();

    private void InitiateGeneralSettings(){
        System.out.println("run");
    }

    private void InitiateCamerasInfo(){

    }
    private void InitiateUsbCameras(){

    }
    private void InitiateUsbCamerasSettings(){

    }
}
