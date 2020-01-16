package com.chameleonvision.networktables;

import com.chameleonvision.config.ConfigManager;
import com.chameleonvision.scripting.ScriptEventType;
import com.chameleonvision.scripting.ScriptManager;
import edu.wpi.first.networktables.LogMessage;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;

import java.util.function.Consumer;

public class NetworkTablesManager {

    private NetworkTablesManager() {}

    private static final NetworkTableInstance NTInst = NetworkTableInstance.getDefault();

    public static final String kRootTableName = "/chameleon-vision";
    public static final NetworkTable kRootTable = NetworkTableInstance.getDefault().getTable(kRootTableName);

    public static boolean isServer = false;

    private static int getTeamNumber() {
        return ConfigManager.settings.teamNumber;
    }

    private static class NTLogger implements Consumer<LogMessage> {

        private boolean hasReportedConnectionFailure = false;

        @Override
        public void accept(LogMessage logMessage) {
            if (!hasReportedConnectionFailure && logMessage.message.contains("timed out")) {
                System.err.println("NT Connection has failed! Will retry in background.");
                hasReportedConnectionFailure = true;
            } else if (logMessage.message.contains("connected")) {
                System.out.println("NT Connected!");
                hasReportedConnectionFailure = false;
                ScriptManager.queueEvent(ScriptEventType.kNTConnected);
            }
        }
    }

    static {
        NetworkTableInstance.getDefault().addLogger(new NTLogger(), 0, 255); // to hide error messages
    }

    public static void setClientMode(String host) {
        isServer = false;
        System.out.println("Starting NT Client");
        NTInst.stopServer();
        if (host != null) {
            NTInst.startClient(host);
        } else {
            NTInst.startClientTeam(getTeamNumber());
        }
    }

    public static void setTeamClientMode() {
        setClientMode(null);
    }

    public static void setServerMode() {
        isServer = true;
        System.out.println("Starting NT Server");
        NTInst.stopClient();
        NTInst.startServer();
    }
}
