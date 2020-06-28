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
