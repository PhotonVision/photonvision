package Classes;

import Classes.MetaClass.Singleton;
import Objects.*;
import org.json.JSONObject;

import java.util.HashMap;

public class SettingsManager extends Singleton  {
    public static int x = 5; // example of how to setup variable in singleton
    public static HashMap cams = new HashMap();
    public static HashMap UsbCameras = new HashMap();
    public static HashMap USBCamerasInfo = new HashMap();
    public static DefaultGeneralSettings GeneralSettings;
    public static HashMap CameraPort = new HashMap();
    public static HashMap CamerasCurrentPipeline = new HashMap();
    public SettingsManager(){
        InitiateGeneralSettings();
        InitiateCamerasInfo();
        InitiateUsbCameras();
        InitiateUsbCamerasSettings();
    }

    private void InitiateGeneralSettings(){

    }
    private void InitiateCamerasInfo(){

    }
    private void InitiateUsbCameras(){

    }
    private void InitiateUsbCamerasSettings(){

    }
}
