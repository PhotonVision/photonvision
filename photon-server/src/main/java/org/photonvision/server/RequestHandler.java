/*
 * Copyright (C) 2020 Photon Vision.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;

public class RequestHandler {

    private static final ObjectMapper kObjectMapper = new ObjectMapper();

    /** Parses and saves general settings to the config manager. */
    public static void onGeneralSettings(Context context) {
        return;
    }

    /** Parses and saves camera settings (FOV and tilt) to the current camera. */
    public static void onCameraSettings(Context context) {
        return;
    }

    /** Duplicates the selected camera */
    public static void onDuplicatePipeline(Context context) {
        return;
    }

    public static void onCalibrationStart(Context context) {
        return;
    }

    public static void onSnapshot(Context context) {
        return;
    }

    public static void onCalibrationEnding(Context context) {
        return;
    }

    /** Parses and saves the current 3d settings to the current pipeline. */
    public static void onPnpModel(Context context) {
        return;
    }

    public static void onInstallOrUpdate(Context context) {
        return;
    }
}
