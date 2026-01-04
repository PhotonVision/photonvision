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

import io.javalin.http.Context;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.configuration.NeuralNetworkModelManager;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.file.JacksonUtils;

public class TestRequestHandler {
    // Treat all 2XX calls as "INFO"
    // Treat all 4XX calls as "ERROR"
    // Treat all 5XX calls as "ERROR"

    static Logger logger = new Logger(TestRequestHandler.class, LogGroup.WebServer);

    public static void handleResetRequest(Context ctx) {
        logger.info("Resetting Backend");
        // Reset backend
        ConfigManager.nukeConfigDirectory();
        ConfigManager.getInstance().load();
    }

    private record PlatformOverrideRequest(Platform platform) {}

    public static void handlePlatformOverrideRequest(Context ctx) {
        try {
            PlatformOverrideRequest request =
                    JacksonUtils.deserialize(ctx.body(), PlatformOverrideRequest.class);
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

    public static void testMode(Context ctx) {
        logger.info("Test mode activated");
        RequestHandler.setTestMode(true);
        ctx.status(200).result("Test mode activated");
    }
}
