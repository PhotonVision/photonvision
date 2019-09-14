import Classes.SettingsManager;
import Handlers.Vision.CameraProcess;
import Handlers.Web.Server;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;

import java.util.Map;

public class Main {
    public static void main(String [] args) {
        SettingsManager manager = SettingsManager.getInstance();
//        NetworkTableInstance.getDefault().startClientTeam(SettingsManager.GeneralSettings.team_number);
        for (Map.Entry<String, UsbCamera> entry: SettingsManager.UsbCameras.entrySet()){
            new Thread(new CameraProcess(entry.getKey())).start();
        }
        Server.main(8888);
    }
}


