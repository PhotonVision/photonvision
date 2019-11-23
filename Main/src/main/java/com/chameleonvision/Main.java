package com.chameleonvision;

import com.chameleonvision.config.ConfigManager;
import com.chameleonvision.network.NetworkManager;
import com.chameleonvision.util.Platform;
import com.chameleonvision.util.Utilities;
import com.chameleonvision.vision.VisionManager;
import com.chameleonvision.web.Server;
import edu.wpi.cscore.CameraServerCvJNI;
import edu.wpi.cscore.CameraServerJNI;
import edu.wpi.first.networktables.LogMessage;
import edu.wpi.first.networktables.NetworkTableInstance;

import java.io.IOException;
import java.util.function.Consumer;

import static com.chameleonvision.util.Platform.CurrentPlatform;

public class Main {

    private static final String NT_SERVERMODE_KEY = "--nt-servermode"; // no args for this setting
    private static final String NT_CLIENTMODESERVER_KEY = "--nt-client-server"; // expects String representing an IP address (hostnames will be rejected!)
    private static final String NETWORK_MANAGE_KEY = "--unmanage-network"; // no args for this setting
    private static final String IGNORE_ROOT_KEY = "--ignore-root"; // no args for this setting
    private static final String TEST_MODE_KEY = "--cv-development";

    private static final int DEFAULT_PORT = 5800;

    private static boolean ntServerMode = false;
    private static boolean manageNetwork = true;
    private static boolean ignoreRoot = false;
    private static String ntClientModeServer = null;
    private static boolean testMode = true;

    private static class NTLogger implements Consumer<LogMessage> {

        private boolean hasReportedConnectionFailure = false;

        @Override
        public void accept(LogMessage logMessage) {
            if (!hasReportedConnectionFailure && logMessage.message.contains("timed out")) {
                System.err.println("NT Connection has failed!");
                hasReportedConnectionFailure = true;
            }
        }
    }

    private static void handleArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            var key = args[i].toLowerCase();
            String value = null;

            // this switch handles arguments with a value. Add any settings with a value here.
            switch (key) {
                case NT_CLIENTMODESERVER_KEY:
                    var potentialValue = args[i + 1];
                    // ensures this "value" isnt null, blank, nor another argument
                    if (potentialValue != null && !potentialValue.isBlank() && !potentialValue.startsWith("-") & !potentialValue.startsWith("--")) {
                        value = potentialValue.toLowerCase();
                    }
                    i++; // increment to skip an 'arg' next go-around of for loop, as that would be this value
                    break;
                case NT_SERVERMODE_KEY:
                case NETWORK_MANAGE_KEY:
                case IGNORE_ROOT_KEY:
                case TEST_MODE_KEY:
                    // nothing
                    break;
            }

            // this switch actually handles the arguments.
            switch (key) {
                case NT_SERVERMODE_KEY:
                    ntServerMode = true;
                    break;
                case NT_CLIENTMODESERVER_KEY:
                    if (value != null) {
                        if (value.equals("localhost")) {
                            ntClientModeServer = "127.0.0.1";
                            return;
                        }

                        if (Utilities.isValidIPV4(value)) {
                            ntClientModeServer = value;
                            return;
                        }
                    }
                    System.err.println("Argument for NT Server Host was invalid, defaulting to team number host");
                    break;
                case NETWORK_MANAGE_KEY:
                    manageNetwork = false;
                    break;
                case IGNORE_ROOT_KEY:
                    ignoreRoot = true;
                    break;
                case TEST_MODE_KEY:
                    testMode = true;
                    break;
            }
        }
    }

    public static void main(String[] args) {
        if (CurrentPlatform.equals(Platform.UNSUPPORTED)) {
            System.err.printf("Sorry, this platform is not supported. Give these details to the developers.\n%s\n", CurrentPlatform.toString());
            return;
        } else {
            System.out.printf("Starting Chameleon Vision on platform %s\n", CurrentPlatform.toString());
        }

        handleArgs(args);

        if (!CurrentPlatform.isRoot()) {
            if (ignoreRoot) {
                // manageNetwork = false;
                System.out.println("Ignoring root, network will not be managed!");
            } else {
                System.err.println("This program must be run as root!");
                    return;
            }
        }

        // Attempt to load the JNI Libraries
        try {
            CameraServerJNI.forceLoad();
            CameraServerCvJNI.forceLoad();
        } catch (UnsatisfiedLinkError | IOException e) {
            if(CurrentPlatform.isWindows()) {
                System.err.println("Try to download the VC++ Redistributable, https://aka.ms/vs/16/release/vc_redist.x64.exe");
            }
            throw new RuntimeException("Failed to load JNI Libraries!");
        }

        ConfigManager.initializeSettings();
        NetworkManager.initialize(manageNetwork);

        if (ntServerMode) {
            System.out.println("Starting NT Server");
            NetworkTableInstance.getDefault().startServer();
        } else {
            NetworkTableInstance.getDefault().addLogger(new NTLogger(), 0, 255); // to hide error messages
            if (ntClientModeServer != null) {
                NetworkTableInstance.getDefault().startClient(ntClientModeServer);
            } else {
                NetworkTableInstance.getDefault().startClientTeam(ConfigManager.settings.teamNumber);
            }
        }

        boolean visionSourcesOk = VisionManager.initializeSources();
        if (!visionSourcesOk) {
            System.out.println("No cameras connected!");
            return;
        }

        boolean visionProcessesOk = VisionManager.initializeProcesses();
        if (!visionProcessesOk) {
            System.err.println("shit");
            return;
        }

        VisionManager.startProcesses();

        System.out.printf("Starting Webserver at port %d\n", DEFAULT_PORT);
        Server.main(DEFAULT_PORT);
    }
}
