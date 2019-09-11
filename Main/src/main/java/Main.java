import Classes.SettingsManager;
import Handlers.Web.Server;
public class Main {
    public static void main(String [] args) {
        SettingsManager manager = SettingsManager.getInstance();
        Server.main(8888);
    }
}


