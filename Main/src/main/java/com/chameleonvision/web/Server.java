package com.chameleonvision.web;

import com.chameleonvision.settings.SettingsManager;
import io.javalin.Javalin;

public class Server {
    private static SocketHandler socketHandler;

    public static void main(int port) {
        socketHandler = new SocketHandler();

        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.showJavalinBanner = false;
            javalinConfig.addStaticFiles("web");
            javalinConfig.enableCorsForAllOrigins();
        });
        app.ws("/websocket", ws -> {
            ws.onConnect(ctx -> {
                socketHandler.onConnect(ctx);
                System.out.println("Socket Connected");
            });
            ws.onClose(ctx -> {
                socketHandler.onClose(ctx);
                System.out.println("Socket Disconnected");
                SettingsManager.saveSettings();
            });
            ws.onBinaryMessage(ctx -> {
                socketHandler.onBinaryMessage(ctx);
            });
        });
        app.post("/api/settings/general", Requesthandler::onGeneralSettings);
        app.post("/api/settings/camera", Requesthandler::onCameraSettings);
        app.start(port);
    }
}