package org.photonvision.common.dataflow.networktables;

import edu.wpi.first.networktables.LogMessage;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.util.function.Consumer;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.scripting.ScriptEventType;
import org.photonvision.common.scripting.ScriptManager;

public class NetworkTablesManager {

    private NetworkTablesManager() {}

    private static final Logger logger = new Logger(NetworkTablesManager.class, LogGroup.General);

    private static final NetworkTableInstance ntInstance = NetworkTableInstance.getDefault();

    public static final String kRootTableName = "/chameleon-vision";
    public static final NetworkTable kRootTable =
            NetworkTableInstance.getDefault().getTable(kRootTableName);

    public static boolean isServer = false;

    private static int getTeamNumber() {
        // TODO: FIX
        return 0;
        //        return ConfigManager.settings.teamNumber;
    }

    private static class NTLogger implements Consumer<LogMessage> {

        private boolean hasReportedConnectionFailure = false;

        @Override
        public void accept(LogMessage logMessage) {
            if (!hasReportedConnectionFailure && logMessage.message.contains("timed out")) {
                logger.error("NT Connection has failed! Will retry in background.");
                hasReportedConnectionFailure = true;
            } else if (logMessage.message.contains("connected")) {
                logger.info("NT Connected!");
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
        logger.info("Starting NT Client");
        ntInstance.stopServer();
        if (host != null) {
            ntInstance.startClient(host);
        } else {
            ntInstance.startClientTeam(getTeamNumber());
            if (ntInstance.isConnected()) {
                logger.info("[NetworkTablesManager] Connected to the robot!");
            } else {
                logger.info(
                        "[NetworkTablesManager] Could NOT to the robot! Will retry in the background...");
            }
        }
    }

    public static void setTeamClientMode() {
        setClientMode(null);
    }

    public static void setServerMode() {
        isServer = true;
        logger.info("Starting NT Server");
        ntInstance.stopClient();
        ntInstance.startServer();
    }
}
