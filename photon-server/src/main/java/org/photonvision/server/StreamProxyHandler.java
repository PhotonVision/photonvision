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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class StreamProxyHandler {
    private static final Logger logger = new Logger(StreamProxyHandler.class, LogGroup.WebServer);

    // Buffer size for streaming data
    private static final int BUFFER_SIZE = 8192;

    /**
     * Handles proxy requests to MJPEG camera streams. Accepts requests at /port/{portNumber}/* and
     * proxies them to localhost:{portNumber}/*
     *
     * @param ctx Javalin context containing the request and response
     */
    public static void handleProxyRequest(Context ctx) {
        String portStr = ctx.pathParam("port");
        int targetPort;

        // Parse and validate port number
        try {
            targetPort = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            ctx.status(400);
            ctx.result("Invalid port number: " + portStr);
            logger.error("Invalid port number requested: " + portStr);
            return;
        }

        // Extract the path after /port/{port}/
        String fullPath = ctx.path();
        String pathAfterPort =
                fullPath.substring(fullPath.indexOf("/" + portStr) + ("/" + portStr).length());

        // If no path specified, default to empty (will connect to root)
        if (pathAfterPort.isEmpty()) {
            pathAfterPort = "/";
        }

        // Construct URL to local MJPEG stream
        String streamUrl = "http://localhost:" + targetPort + pathAfterPort;
        logger.debug("Proxying request to: " + streamUrl);

        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            // Open connection to local MJPEG server
            URL url = new URL(streamUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 second connection timeout
            connection.setReadTimeout(0); // No read timeout for streaming

            // Check if connection was successful
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                ctx.status(responseCode);
                ctx.result("Upstream server returned status: " + responseCode);
                logger.error(
                        "Failed to connect to stream at port "
                                + targetPort
                                + ", response code: "
                                + responseCode);
                return;
            }

            // Get content type from upstream server (should be multipart/x-mixed-replace)
            String contentType = connection.getContentType();
            if (contentType != null) {
                ctx.contentType(contentType);
            } else {
                // Fallback to standard MJPEG content type
                ctx.contentType("multipart/x-mixed-replace; boundary=--jpgboundary");
            }

            // Set response status
            ctx.status(200);

            // Get streams
            inputStream = connection.getInputStream();
            outputStream = ctx.res().getOutputStream();

            // Stream data from MJPEG server to client
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }

        } catch (IOException e) {
            // Connection errors are common when clients disconnect, log at debug level
            logger.debug("Error proxying stream from port " + targetPort + ": " + e.getMessage());

            // Try to send error response if we haven't started streaming yet
            if (!ctx.res().isCommitted()) {
                ctx.status(502);
                ctx.result("Failed to connect to stream: " + e.getMessage());
            }
        } finally {
            // Clean up resources
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.trace("Error closing input stream: " + e.getMessage());
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.trace("Error closing output stream: " + e.getMessage());
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
