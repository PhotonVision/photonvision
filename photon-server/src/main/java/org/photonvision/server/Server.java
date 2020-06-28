package org.photonvision.server;

import io.javalin.Javalin;

public class Server {

    public static void main(int port) {
        Javalin app =
                Javalin.create(
                        javalinConfig -> {
                            javalinConfig.showJavalinBanner = false;
                            javalinConfig.addStaticFiles("web");
                            javalinConfig.enableCorsForAllOrigins();
                        });
        /*Web Socket Events */
        app.ws(
                "/websocket",
                ws -> {
                    ws.onConnect(SocketHandler::onConnect);
                    ws.onClose(SocketHandler::onClose);
                    ws.onBinaryMessage(SocketHandler::onBinaryMessage);
                });
        /*API Events*/
        app.post("/api/settings/general", RequestHandler::onGeneralSettings);
        app.post("/api/settings/camera", RequestHandler::onCameraSettings);
        app.post("/api/vision/duplicate", RequestHandler::onDuplicatePipeline);
        app.post("/api/settings/startCalibration", RequestHandler::onCalibrationStart);
        app.post("/api/settings/snapshot", RequestHandler::onSnapshot);
        app.post("/api/settings/endCalibration", RequestHandler::onCalibrationEnding);
        app.post("/api/vision/pnpModel", RequestHandler::onPnpModel);
        app.post("/api/install", RequestHandler::onInstallOrUpdate);
        app.start(port);
    }
}
