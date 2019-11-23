package com.chameleonvision.classabstraction.config;

import com.chameleonvision.classabstraction.camera.USBCameraProperties;
import com.chameleonvision.classabstraction.camera.USBCameraProcess;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class CameraSerializer extends StdSerializer<USBCameraProcess> {
    public CameraSerializer(Class<USBCameraProcess> t) {
        super(t);
    }

    @Override
    public void serialize(USBCameraProcess value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        USBCameraProperties camProps = value.getProperties();
        gen.writeNumberField("FOV", camProps.FOV);
        gen.writeStringField("Name", camProps.name);
        gen.writeStringField("Path", camProps.path);
        gen.writeStringField("Nickname", camProps.getNickname());
    }
}
