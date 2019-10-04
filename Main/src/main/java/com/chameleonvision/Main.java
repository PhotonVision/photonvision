package com.chameleonvision;

import com.chameleonvision.settings.Platform;
import com.chameleonvision.settings.SettingsManager;
import com.chameleonvision.settings.network.NetworkManager;
import com.chameleonvision.util.Utilities;
import com.chameleonvision.vision.camera.CameraManager;
import com.chameleonvision.web.Server;
import edu.wpi.cscore.CameraServerCvJNI;
import edu.wpi.cscore.CameraServerJNI;
import edu.wpi.first.networktables.NetworkTableInstance;

import java.io.IOException;

public class Main {

    private static final String PORT_KEY = "--port"; // expects integer
    private static final String NT_SERVERMODE_KEY = "--nt-servermode"; // no args for this setting
    private static final String NT_CLIENTMODESERVER_KEY = "--nt-client-server"; // expects String representing an IP address (hostnames will be rejected!)
    private static final String NETWORK_MANAGE_KEY = "--unmanage-network"; // no args for this setting

    private static final int DEFAULT_PORT = 8888;

    private static int webserverPort = DEFAULT_PORT;
    private static boolean ntServerMode = false;
    private static boolean manageNetwork = true;
    private static String ntClientModeServer = null;

    private static void handleArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            var key = args[i].toLowerCase();
            String value = null;

            // this switch handles arguments with a value. Add any settings with a value here.
            switch (key) {
                case PORT_KEY:
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
                    // nothing
            }

            // this switch actually handles the arguments.
            switch (key) {
                case PORT_KEY:
                    System.out.println("INFO - The \"--port\" argument is currently disabled.");
//                    try {
//                        if (value == null) throw new Exception("Bad or No argument value");
//                        webserverPort = Integer.parseInt(value);
//                    } catch (Exception ex) {
//                        System.err.printf("Argument for port was invalid, starting server at port %d\n", DEFAULT_PORT);
//                    }
                    break;
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
            }
        }
    }

    public static void main(String[] args) {
        handleArgs(args);

        // Attempt to load the JNI Libraries
        try {
            CameraServerJNI.forceLoad();
            CameraServerCvJNI.forceLoad();
        } catch (IOException e) {
            var errorStr = Platform.getCurrentPlatform().equals(Platform.UNSUPPORTED) ? "Unsupported platform!" : "Failed to load JNI Libraries!";
            throw new RuntimeException(errorStr);
        }

        if (CameraManager.initializeCameras()) {
            SettingsManager.initialize(manageNetwork);
            CameraManager.initializeThreads();

            if (ntServerMode) {
                System.out.println("Starting NT Server");
                NetworkTableInstance.getDefault().startServer();
            } else {
                if (ntClientModeServer != null) {
                    NetworkTableInstance.getDefault().startClient(ntClientModeServer);
                } else {
                    NetworkTableInstance.getDefault().startClientTeam(SettingsManager.GeneralSettings.team_number);
                }
            }

            System.out.printf("Starting Webserver at port %d\n", webserverPort);
            Server.main(webserverPort);
        } else {
            System.err.println("No cameras connected!");
        }
    }
}
