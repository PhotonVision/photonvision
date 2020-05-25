package com.chameleonvision.server;

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
        //        app.post("/api/settings/general",
        // com.chameleonvision._2.web.RequestHandler::onGeneralSettings);
        //        app.post("/api/settings/camera",
        // com.chameleonvision._2.web.RequestHandler::onCameraSettings);
        //        app.post("/api/vision/duplicate",
        // com.chameleonvision._2.web.RequestHandler::onDuplicatePipeline);
        //        app.post("/api/settings/startCalibration",
        // com.chameleonvision._2.web.RequestHandler::onCalibrationStart);
        //        app.post("/api/settings/snapshot",
        // com.chameleonvision._2.web.RequestHandler::onSnapshot);
        //        app.post("/api/settings/endCalibration",
        // com.chameleonvision._2.web.RequestHandler::onCalibrationEnding);
        //        app.post("/api/vision/pnpModel",
        // com.chameleonvision._2.web.RequestHandler::onPnpModel);
        //        app.post("/api/install", RequestHandler::onInstallOrUpdate);
        app.start(port);
    }
}
