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
import java.util.List;
import java.util.StringJoiner;
import org.photonvision.common.dataflow.DataChangeDestination;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.DataChangeSource;
import org.photonvision.common.dataflow.DataChangeSubscriber;
import org.photonvision.common.dataflow.events.DataChangeEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class Server {
    private static final Logger logger = new Logger(Server.class, LogGroup.WebServer);

    private static Javalin app = null;

    static class RestartSubscriber extends DataChangeSubscriber {
        private RestartSubscriber() {
            super(DataChangeSource.AllSources, List.of(DataChangeDestination.DCD_WEBSERVER));
        }

        @Override
        public void onDataChangeEvent(DataChangeEvent<?> event) {
            if (event.propertyName.equals("restartServer")) {
                Server.restart();
            }
        }
    }

    public static void initialize(int port) {
        DataChangeService.getInstance().addSubscriber(new RestartSubscriber());

        start(port);
    }

    private static void start(int port) {
        app =
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
                                                        .add("of req size")
                                                        .add(Integer.toString(ctx.contentLength()))
                                                        .add("bytes & type")
                                                        .add(ctx.contentType())
                                                        .add("with return code")
                                                        .add(Integer.toString(ctx.res().getStatus()))
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

        /* Web Socket Events for Data Exchange */
        var dsHandler = DataSocketHandler.getInstance();
        app.ws(
                "/websocket_data",
                ws -> {
                    ws.onConnect(dsHandler::onConnect);
                    ws.onClose(dsHandler::onClose);
                    ws.onBinaryMessage(dsHandler::onBinaryMessage);
                });

        /* API Events */
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
        app.get("/api/settings/camera/getCalibImages", RequestHandler::onCameraCalibImagesRequest);

        // Utilities
        app.post("/api/utils/offlineUpdate", RequestHandler::onOfflineUpdateRequest);
        app.post(
                "/api/utils/importObjectDetectionModel",
                RequestHandler::onImportObjectDetectionModelRequest);
        app.get("/api/utils/photonvision-journalctl.txt", RequestHandler::onLogExportRequest);
        app.post("/api/utils/restartProgram", RequestHandler::onProgramRestartRequest);
        app.post("/api/utils/restartDevice", RequestHandler::onDeviceRestartRequest);
        app.post("/api/utils/publishMetrics", RequestHandler::onMetricsPublishRequest);
        app.get("/api/utils/getImageSnapshots", RequestHandler::onImageSnapshotsRequest);
        app.get("/api/utils/getCalSnapshot", RequestHandler::onCalibrationSnapshotRequest);
        app.get("/api/utils/getCalibrationJSON", RequestHandler::onCalibrationExportRequest);
        app.post("/api/utils/nukeConfigDirectory", RequestHandler::onNukeConfigDirectory);
        app.post("/api/utils/nukeOneCamera", RequestHandler::onNukeOneCamera);
        app.post("/api/utils/activateMatchedCamera", RequestHandler::onActivateMatchedCameraRequest);
        app.post("/api/utils/assignUnmatchedCamera", RequestHandler::onAssignUnmatchedCameraRequest);
        app.post("/api/utils/unassignCamera", RequestHandler::onUnassignCameraRequest);

        // Calibration
        app.post("/api/calibration/end", RequestHandler::onCalibrationEndRequest);
        app.post("/api/calibration/importFromData", RequestHandler::onDataCalibrationImportRequest);

        app.start(port);
    }

    /**
     * Seems like if we change the static IP of this device, Javalin refuses to tell us when new
     * Websocket clients connect. As a hack, we can restart the server every time we change static IPs
     */
    public static void restart() {
        logger.info("Web server going down for restart");
        int oldPort = app.port();
        app.stop();
        start(oldPort);
    }
}
