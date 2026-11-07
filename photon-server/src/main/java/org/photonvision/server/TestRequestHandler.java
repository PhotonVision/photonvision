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

import io.avaje.jsonb.Json;
import io.avaje.jsonb.Jsonb;
import io.javalin.http.Context;
import java.util.Map;
import org.photonvision.common.configuration.ConfigProvider;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.camera.CameraQuirk;
import org.photonvision.vision.processes.VisionSourceManager;

public class TestRequestHandler {
    // Treat all 2XX calls as "INFO"
    // Treat all 4XX calls as "ERROR"
    // Treat all 5XX calls as "ERROR"

    static Logger logger = new Logger(TestRequestHandler.class, LogGroup.WebServer);

    public static void handleResetRequest(Context ctx) {
        logger.info("Resetting Backend");
        // Reset backend
        ConfigProvider.nukeConfigDirectory();
        ConfigProvider.getInstance().load();
    }

    @Json
    record PlatformOverrideRequest(Platform platform) {}

    public static void handlePlatformOverrideRequest(Context ctx) {
        try {
            PlatformOverrideRequest request =
                    Jsonb.instance().type(PlatformOverrideRequest.class).fromJson(ctx.body());
            Platform platform = request.platform();
            logger.info("Overriding platform to: " + platform);

            Platform.overridePlatform(platform);
            NeuralNetworkModelManager.getInstance(true).extractModels();
            NeuralNetworkModelManager.getInstance().discoverModels();
            ctx.status(200);

        } catch (Exception e) {
            logger.error("Failed to parse platform override request: " + e.getMessage());
            ctx.status(400).result("Invalid request");
        }
    }

    @Json
    record QuirksOverrideRequest(Map<CameraQuirk, Boolean> quirks) {}

    public static void handleQuirksOverrideRequest(Context ctx) {
        try {
            QuirksOverrideRequest request =
                    Jsonb.instance().type(QuirksOverrideRequest.class).fromJson(ctx.body());
            Map<CameraQuirk, Boolean> quirks = request.quirks();
            logger.info("Overriding quirks on all cameras to: " + quirks);

            VisionSourceManager.getInstance()
                    .vmm
                    .getModules()
                    .forEach(module -> module.changeCameraQuirks(quirks));
            ctx.status(200);

        } catch (Exception e) {
            logger.error("Failed to parse quirks override request: " + e.getMessage());
            ctx.status(400).result("Invalid request");
        }
    }

    public static void testMode(Context ctx) {
        logger.info("Test mode activated");
        RequestHandler.setTestMode(true);
        ctx.status(200).result("Test mode activated");
    }
}
