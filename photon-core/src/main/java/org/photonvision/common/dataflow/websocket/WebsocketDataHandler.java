package org.photonvision.common.dataflow.websocket;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.websocket.WsBinaryMessageContext;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.photonvision.PhotonVersion;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.DataChangeSource;
import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.dataflow.events.IncomingWebSocketEvent;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.hardware.HardwareManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.networking.NetworkUtils;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.jni.RknnDetectorJNI;
import org.photonvision.mrcal.MrCalJNILoader;
import org.photonvision.raspi.LibCameraJNILoader;

public class WebsocketDataHandler {
    private static final boolean USE_MPACK = true;
    private final Logger logger = new Logger(WebsocketDataHandler.class, LogGroup.WebServer);

    private final List<WsContext> clients = new CopyOnWriteArrayList<>();
    private final ObjectMapper jsonMapper = new ObjectMapper(new JsonFactory());
    private final ObjectMapper messagePackMapper = new ObjectMapper(new MessagePackFactory());

    private static WebsocketDataHandler instance;

    public static WebsocketDataHandler getInstance() {
        if (instance == null) {
            instance = new WebsocketDataHandler();
        }
        return instance;
    }

    public WebsocketDataHandler() {
        DataChangeService.getInstance()
                .addSubscribers(new InboundSubscriber(), new OutboundSubscriber());
    }

    public void onClientConnection(WsConnectContext context) {
        clients.add(context);
        // Disable idle timeout disconnection
        context.session.setIdleTimeout(Duration.ofMillis(Long.MAX_VALUE));

        var remote = (InetSocketAddress) context.session.getRemoteAddress();
        var host = remote.getAddress().toString() + ":" + remote.getPort();
        logger.info("New websocket client connection from " + host);
    }

    public void onClientDisconnect(WsCloseContext context) {
        clients.remove(context);
        var remote = (InetSocketAddress) context.session.getRemoteAddress();
        // Remote can be null if server is being closed for restart
        if (remote != null) {
            var host = remote.getAddress().toString() + ":" + remote.getPort();
            var reason = context.reason() != null ? context.reason() : "Connection closed by client";
            logger.info("Closing websocket connection from " + host + " for reason: " + reason);
        } else {
            logger.info("Closing websockets for user " + context.getSessionId());
        }
    }

    public void onBinaryMessage(WsBinaryMessageContext context) {}

    public void sendMessageToClient(Object message, WsContext client) {
        if (!client.session.isOpen()) return;

        ByteBuffer buffered_message;
        try {
            if(USE_MPACK) {
                buffered_message = ByteBuffer.wrap(messagePackMapper.writeValueAsBytes(message));
            } else {
                buffered_message = ByteBuffer.wrap(jsonMapper.writeValueAsBytes(message));
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message", e);
            return;
        }

        client.send(message);
    }

    public void broadcastMessage(Object message, WsContext clientToSkip) {
        if (clientToSkip == null) {
            for (WsContext user : clients) {
                sendMessageToClient(message, user);
            }
        } else {
            var skipUserPort = ((InetSocketAddress) clientToSkip.session.getRemoteAddress()).getPort();
            for (WsContext client : clients) {
                var userPort = ((InetSocketAddress) client.session.getRemoteAddress()).getPort();
                if (userPort != skipUserPort) {
                    sendMessageToClient(message, client);
                }
            }
        }
    }

    private Object getInstanceConfig() {
        var instanceConfigSubmap = new HashMap<String, Object>();
        instanceConfigSubmap.put("version", PhotonVersion.versionString);
        instanceConfigSubmap.put("gpuAccelerationSupported", LibCameraJNILoader.isSupported());
        instanceConfigSubmap.put("mrCalWorking", MrCalJNILoader.getInstance().isLoaded());
        instanceConfigSubmap.put("rknnSupported", RknnDetectorJNI.getInstance().isLoaded());
        instanceConfigSubmap.put("hardwareModel", ConfigManager.getInstance().getConfig().getHardwareConfig().deviceName);
        instanceConfigSubmap.put("hardwarePlatform", Platform.getPlatformName());
        return instanceConfigSubmap;
    }

    private Object getSettings() {
        var settingsSubmap = new HashMap<String, Object>();

        // Lighting Settings
        var lightingSettingsSubmap = new HashMap<String, Object>();
        lightingSettingsSubmap.put("brightness", ConfigManager.getInstance().getConfig().getHardwareSettings().ledBrightnessPercentage);
        lightingSettingsSubmap.put("supported", !ConfigManager.getInstance().getConfig().getHardwareConfig().ledPins.isEmpty());
        settingsSubmap.put("lighting", lightingSettingsSubmap);

        // Network Settings
        var networkSettingsSubmap = ConfigManager.getInstance().getConfig().getNetworkConfig().toHashMap();
        networkSettingsSubmap.put("networkInterfaceNames", NetworkUtils.getAllWiredInterfaces());
        networkSettingsSubmap.put("networkingDisabled", NetworkManager.getInstance().networkingIsDisabled);
        settingsSubmap.put("network", networkSettingsSubmap);

        // Misc Settings
        var miscSettingsSubmap = SerializationUtils.objectToHashMap(ConfigManager.getInstance().getConfig().getMiscSettings());
        settingsSubmap.put("misc", miscSettingsSubmap);

        return settingsSubmap;
    }

    private Object getATFL() {
        return ConfigManager.getInstance().getConfig().getApriltagFieldLayout();
    }

    private Object getCameraConfigs() {
        return null;
    }

    private static class InboundSubscriber extends DataChangeSubscriber {
        public InboundSubscriber() {
            super(List.of(DataChangeSource.DCS_WEBSOCKET), List.of(DataChangeDestination.DCD_PROGRAM));
        }

        @Override
        public void onDataChangeEvent(DataChangeEvent<?> event) {
            if (event instanceof IncomingWebSocketEvent<?> incomingWSEvent) {
                switch (incomingWSEvent.propertyName) {
                        // TODO
                    case "userConnected" -> {
                        //
                        // DataChangeService.getInstance().publishEvent(
                        //                                                        new
                        // OutgoingUIEvent<>("fullsettings",
                        //                         ConfigManager.getInstance().getConfig().toHashMap(),
                        // incomingWSEvent.originContext)
                        //                                                );
                        Logger.sendConnectedBacklog();
                        NetworkTablesManager.getInstance().broadcastConnectedStatus();
                    }
                    case "ledPercentage" -> {
                        HardwareManager.getInstance().setBrightnessPercent((Integer) incomingWSEvent.data);
                    }
                    case "restartProgram" -> {
                        HardwareManager.getInstance().restartProgram();
                    }
                    case "restartDevice" -> {
                        HardwareManager.getInstance().restartDevice();
                    }
                    case "publishMetrics" -> {
                        HardwareManager.getInstance().publishMetrics();
                    }
                }
            }
        }
    }

    private static class OutboundSubscriber extends DataChangeSubscriber {
        public OutboundSubscriber() {
            super(DataChangeSource.AllSources, List.of(DataChangeDestination.DCD_WEBSOCKET));
        }

        @Override
        public void onDataChangeEvent(DataChangeEvent<?> event) {
            if (event instanceof OutgoingUIEvent<?> thisEvent) {
                if (event.data instanceof HashMap<?, ?> data) {
                    WebsocketDataHandler.getInstance().broadcastMessage(data, thisEvent.originContext);
                } else {
                    WebsocketDataHandler.getInstance().broadcastMessage(event.data, thisEvent.originContext);
                }
            }
        }
    }
}
