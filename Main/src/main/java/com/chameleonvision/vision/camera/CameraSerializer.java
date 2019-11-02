package com.chameleonvision.vision.camera;
import com.google.gson.*;

import java.lang.reflect.Type;

public class CameraSerializer implements JsonSerializer<USBCamera> {
    @Override
    public JsonElement serialize(USBCamera USBCamera, Type type, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("FOV", USBCamera.getFOV());
        obj.addProperty("path", USBCamera.path);
        obj.addProperty("name", USBCamera.name);
        obj.addProperty("nickname", USBCamera.getNickname());
        obj.addProperty("streamDivisor", USBCamera.getStreamDivisor().ordinal());
        var pipelines = context.serialize(USBCamera.getPipelines());
        obj.add("pipelines", pipelines);
        obj.addProperty("resolution", USBCamera.getVideoModeIndex());
        obj.add("camVideoMode", context.serialize(USBCamera.getVideoMode()));
        obj.add("isDriver",context.serialize(USBCamera.getDriverMode()));
        obj.add("driverExposure",context.serialize(USBCamera.getDriverExposure()));
        obj.add("driverBrightness",context.serialize(USBCamera.getDriverBrightness()));
        return obj;
    }
}
