package com.chameleonvision.vision.camera;
import com.google.gson.*;

import java.lang.reflect.Type;

public class CameraSerializer implements JsonSerializer<Camera> {
    @Override
    public JsonElement serialize(Camera camera, Type type, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("FOV", camera.getFOV());
        obj.addProperty("path", camera.path);
        obj.addProperty("name", camera.name);

        var pipelines = context.serialize(camera.getPipelines());
        obj.add("pipelines", pipelines);

        var videoModeIndex = camera.getVideoModeIndex();
        obj.addProperty("resolution", camera.getVideoModeIndex());
        obj.add("camVideoMode", context.serialize(camera.getVideoMode()));

        return obj;
    }
}
