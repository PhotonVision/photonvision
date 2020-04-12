package com.chameleonvision._2;

import static com.chameleonvision.common.util.Platform.CurrentPlatform;

import com.chameleonvision._2.config.ConfigManager;
import com.chameleonvision._2.vision.VisionManager;
import com.chameleonvision._2.web.Server;
import com.chameleonvision.common.datatransfer.networktables.NetworkTablesManager;
import com.chameleonvision.common.networking.NetworkManager;
import com.chameleonvision.common.scripting.ScriptEventType;
import com.chameleonvision.common.scripting.ScriptManager;
import com.chameleonvision.common.util.Platform;
import com.chameleonvision.common.util.math.IPUtils;
import edu.wpi.cscore.CameraServerCvJNI;
import edu.wpi.cscore.CameraServerJNI;
import java.io.IOException;

public class Main {

    private static final String NT_SERVERMODE_KEY = "--nt-servermode"; // no args for this setting
    private static final String NT_CLIENTMODESERVER_KEY =
            "--nt-client-server"; // expects String representing an IP address (hostnames will be
    // rejected!)
    private static final String NETWORK_MANAGE_KEY = "--unmanage-network"; // no args for this setting
    private static final String IGNORE_ROOT_KEY = "--ignore-root"; // no args for this setting
    private static final String TEST_MODE_KEY = "--cv-development";
    private static final String UI_PORT_KEY = "--ui-port";

    private static final int DEFAULT_PORT = 5800;

    private static boolean ntServerMode = false;
    private static boolean manageNetwork = true;
    private static boolean ignoreRoot = false;
    private static String ntClientModeServer = null;
    public static boolean testMode = false;
    public static int uiPort = DEFAULT_PORT;

    private static void handleArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            var key = args[i].toLowerCase();
            String value = null;

            // this switch handles arguments with a value. Add any settings with a value here.
            switch (key) {
                case NT_CLIENTMODESERVER_KEY:
                    var potentialValue = args[i + 1];
                    // ensures this "value" isnt null, blank, nor another argument
                    if (potentialValue != null
                            && !potentialValue.isBlank()
                            && !potentialValue.startsWith("-") & !potentialValue.startsWith("--")) {
                        value = potentialValue.toLowerCase();
                    }
                    i++; // increment to skip an 'arg' next go-around of for loop, as that would be this value
                    break;
                case UI_PORT_KEY:
                    var potentialPort = args[i + 1];
                    if (potentialPort != null
                            && !potentialPort.isBlank()
                            && !potentialPort.startsWith("-") & !potentialPort.startsWith("--")) {
                        value = potentialPort;
                    }
                    i++;
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
                            continue;
                        }

                        if (IPUtils.isValidIPV4(value)) {
                            ntClientModeServer = value;
                            continue;
                        }
                    }
                    System.err.println(
                            "Argument for NT Server Host was invalid, defaulting to team number host");
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
                case UI_PORT_KEY:
                    if (value != null) {
                        try {
                            uiPort = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            System.err.println("ui Port was not a valid number using port 5800");
                        }
                    }
                    break;
            }
        }
    }

    public static void main(String[] args) {

        Runtime.getRuntime()
                .addShutdownHook(new Thread(() -> ScriptManager.queueEvent(ScriptEventType.kProgramExit)));

        if (CurrentPlatform.equals(Platform.UNSUPPORTED)) {
            System.err.printf(
                    "Sorry, this platform is not supported. Give these details to the developers.\n%s\n",
                    CurrentPlatform.toString());
            return;
        } else {
            System.out.printf("Starting Chameleon Vision on platform %s\n", CurrentPlatform.toString());
        }

        handleArgs(args);

        if (!CurrentPlatform.isRoot) {
            if (ignoreRoot) {
                // manageNetwork = false;
                System.out.println("Ignoring root, network will not be managed!");
            } else {
                System.err.println("This program must be run as root!");
                return;
            }
        }

        // Attempt to load the JNI Libraries
        System.out.println("Loading CameraServer...");
        try {
            CameraServerJNI.forceLoad();
            CameraServerCvJNI.forceLoad();
        } catch (UnsatisfiedLinkError | IOException e) {
            if (CurrentPlatform.isWindows()) {
                System.err.println(
                        "Try to download the VC++ Redistributable, https://aka.ms/vs/16/release/vc_redist.x64.exe");
            }
            throw new RuntimeException("Failed to load JNI Libraries!");
        }

        System.out.println("Checking Settings...");
        ConfigManager.initializeSettings();

        if (!CurrentPlatform.isWindows()) {
            System.out.println("Initializing Script Manager...");
            ScriptManager.initialize();
        } else {
            System.out.println("Scripts not yet supported on Windows. ScriptEvents will be ignored.");
        }

        NetworkManager.getInstance().initialize(manageNetwork);

        if (ntServerMode) {
            NetworkTablesManager.setServerMode();
        } else {
            NetworkTablesManager.setClientMode(ntClientModeServer);
        }

        ScriptManager.queueEvent(ScriptEventType.kProgramInit);

        boolean visionSourcesOk = VisionManager.initializeSources();
        if (!visionSourcesOk) {
            System.err.println("No cameras connected!");
            return;
        }

        boolean visionProcessesOk = VisionManager.initializeProcesses();
        if (!visionProcessesOk) {
            System.err.println("Failed to initialize vision processes!");
            return;
        }

        System.out.println("Starting vision processes...");
        VisionManager.startProcesses();

        System.out.printf("Starting Web server at port %d\n", uiPort);
        Server.main(uiPort);
    }
}
