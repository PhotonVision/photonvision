/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.photonvision.server;

import io.javalin.Javalin;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class Server {
    private static final Logger logger = new Logger(Server.class, LogGroup.WebServer);

    public static void main(int port) {
        Javalin app =
                Javalin.create(
                        config -> {
                            config.showJavalinBanner = false;
                            config.addStaticFiles("web");
                            config.enableCorsForAllOrigins();

                            config.requestLogger(
                                    (ctx, ms) ->
                                            logger.debug(
                                                    "Handled HTTP "
                                                            + ctx.req.getMethod()
                                                            + " request from "
                                                            + ctx.req.getRemoteHost()
                                                            + " in "
                                                            + ms.toString()
                                                            + "ms"));

                            config.wsLogger(
                                    ws ->
                                            ws.onMessage(
                                                    ctx -> logger.debug("Got WebSockets message: " + ctx.message())));

                            config.wsLogger(
                                    ws ->
                                            ws.onBinaryMessage(
                                                    ctx ->
                                                            logger.trace(
                                                                    () -> {
                                                                        var insa = ctx.session.getRemote().getInetSocketAddress();
                                                                        var host = insa.getAddress().toString() + ":" + insa.getPort();
                                                                        return "Got WebSockets binary message from host " + host;
                                                                    })));
                        });

        var socketHandler = SocketHandler.getInstance();

        /*Web Socket Events */
        app.ws(
                "/websocket",
                ws -> {
                    ws.onConnect(socketHandler::onConnect);
                    ws.onClose(socketHandler::onClose);
                    ws.onBinaryMessage(socketHandler::onBinaryMessage);
                });
        /*API Events*/
        app.post("/api/settings/import", RequestHandler::onSettingUpload);
        app.get("/api/settings/photonvision_config.zip", RequestHandler::onSettingsDownload);
        app.post("/api/settings/camera", RequestHandler::onCameraSettingsSave);
        app.post("/api/settings/general", RequestHandler::onGeneralSettings);
        app.post("/api/settings/endCalibration", RequestHandler::onCalibrationEnd);
        app.post("/api/restartDevice", RequestHandler::restartDevice);
        app.post("api/restartProgram", RequestHandler::restartProgram);
        app.post("api/vision/pnpModel", RequestHandler::uploadPnpModel);
        app.post("api/sendMetrics", RequestHandler::sendMetrics);
        app.post("api/setCameraNickname", RequestHandler::setCameraNickname);

        app.get("api/deleteSnapshot", RequestHandler::deleteSnapshot);
        app.get("api/selectSnapshot", RequestHandler::selectSnapshot);
        app.get("api/getSnapshot", RequestHandler::getSnapshot);
        app.get("api/allSnapshots", RequestHandler::getSnapshotList);

        app.start(port);
    }
}
