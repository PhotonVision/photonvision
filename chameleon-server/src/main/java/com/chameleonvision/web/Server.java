package com.chameleonvision.web;

import com.chameleonvision.config.ConfigManager;
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
                ConfigManager.saveGeneralSettings();
            });
            ws.onBinaryMessage(ctx -> {
                socketHandler.onBinaryMessage(ctx);
            });
        });
        app.post("/api/settings/general", RequestHandler::onGeneralSettings);
        app.post("/api/settings/camera", RequestHandler::onCameraSettings);
        app.post("/api/vision/duplicate", RequestHandler::onDuplicatePipeline);
        app.post("/api/settings/startCalibration", RequestHandler::onCalibrationStart);
        app.post("/api/settings/snapshot", RequestHandler::onSnapshot);
        app.post("/api/settings/endCalibration", RequestHandler::onCalibrationEnding);
        app.post("/api/vision/pnpModel", RequestHandler::onPnpModel);
        app.start(port);
    }
}
