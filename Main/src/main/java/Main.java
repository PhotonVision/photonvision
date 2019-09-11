import Classes.SettingsManager;
import Handlers.Web.Server;
public class Main {
    public static void main(String [] args) {
        SettingsManager Manager = new SettingsManager();
        Server.main(8888);
    }
}


