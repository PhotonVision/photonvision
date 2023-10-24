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
import io.javalin.plugin.bundled.CorsPluginConfig;
import java.net.InetSocketAddress;
import java.util.StringJoiner;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class Server {
  private static final Logger logger = new Logger(Server.class, LogGroup.WebServer);

  public static void start(int port) {
    var app =
        Javalin.create(
            javalinConfig -> {
              javalinConfig.showJavalinBanner = false;
              javalinConfig.staticFiles.add("web");
              javalinConfig.plugins.enableCors(
                  corsContainer -> {
                    corsContainer.add(CorsPluginConfig::anyHost);
                  });

              javalinConfig.requestLogger.http(
                  (ctx, ms) -> {
                    StringJoiner joiner =
                        new StringJoiner(" ")
                            .add("Handled HTTP request of type")
                            .add(ctx.req().getMethod())
                            .add("from endpoint")
                            .add(ctx.path())
                            .add("for host")
                            .add(ctx.req().getRemoteHost())
                            .add("in")
                            .add(ms.toString())
                            .add("ms");

                    logger.debug(joiner.toString());
                  });
              javalinConfig.requestLogger.ws(
                  ws -> {
                    ws.onMessage(ctx -> logger.debug("Got WebSockets message: " + ctx.message()));
                    ws.onBinaryMessage(
                        ctx ->
                            logger.trace(
                                () -> {
                                  var remote = (InetSocketAddress) ctx.session.getRemoteAddress();
                                  var host =
                                      remote.getAddress().toString() + ":" + remote.getPort();
                                  return "Got WebSockets binary message from host: " + host;
                                }));
                  });
            });

    /*Web Socket Events for Data Exchange */
    var dsHandler = DataSocketHandler.getInstance();
    app.ws(
        "/websocket_data",
        ws -> {
          ws.onConnect(dsHandler::onConnect);
          ws.onClose(dsHandler::onClose);
          ws.onBinaryMessage(dsHandler::onBinaryMessage);
        });

    /*Web Socket Events for Camera Streaming */
    var camDsHandler = CameraSocketHandler.getInstance();
    app.ws(
        "/websocket_cameras",
        ws -> {
          ws.onConnect(camDsHandler::onConnect);
          ws.onClose(camDsHandler::onClose);
          ws.onBinaryMessage(camDsHandler::onBinaryMessage);
          ws.onMessage(camDsHandler::onMessage);
        });

    /*API Events*/
    // Settings
    app.post("/api/settings", RequestHandler::onSettingsImportRequest);
    app.get("/api/settings/photonvision_config.zip", RequestHandler::onSettingsExportRequest);
    app.post("/api/settings/hardwareConfig", RequestHandler::onHardwareConfigRequest);
    app.post("/api/settings/hardwareSettings", RequestHandler::onHardwareSettingsRequest);
    app.post("/api/settings/networkConfig", RequestHandler::onNetworkConfigRequest);
    app.post("/api/settings/aprilTagFieldLayout", RequestHandler::onAprilTagFieldLayoutRequest);
    app.post("/api/settings/general", RequestHandler::onGeneralSettingsRequest);
    app.post("/api/settings/camera", RequestHandler::onCameraSettingsRequest);
    app.post("/api/settings/camera/setNickname", RequestHandler::onCameraNicknameChangeRequest);

    // Utilities
    app.post("/api/utils/offlineUpdate", RequestHandler::onOfflineUpdateRequest);
    app.get("/api/utils/photonvision-journalctl.txt", RequestHandler::onLogExportRequest);
    app.post("/api/utils/restartProgram", RequestHandler::onProgramRestartRequest);
    app.post("/api/utils/restartDevice", RequestHandler::onDeviceRestartRequest);
    app.post("/api/utils/publishMetrics", RequestHandler::onMetricsPublishRequest);

    // Calibration
    app.post("/api/calibration/end", RequestHandler::onCalibrationEndRequest);
    app.post("/api/calibration/importFromCalibDB", RequestHandler::onCalibrationImportRequest);

    app.start(port);
  }
}
