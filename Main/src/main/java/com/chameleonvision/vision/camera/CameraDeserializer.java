package com.chameleonvision.vision.camera;

import com.chameleonvision.vision.Pipeline;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CameraDeserializer implements JsonDeserializer<Camera> {
	@Override
	public Camera deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
		var jsonObj = jsonElement.getAsJsonObject();
		var camFOV = jsonObj.get("FOV").getAsDouble();
		var camName = jsonObj.get("name").getAsString();
		var camNickname = jsonObj.get("nickname").getAsString();
		var videoModeIndex = jsonObj.get("resolution").getAsInt();
		var divisor = StreamDivisor.values()[jsonObj.get("streamDivisor").getAsInt()];

		var pipelines = jsonObj.get("pipelines");
		List<Pipeline> actualPipelines = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		TypeFactory typeFactory = mapper.getTypeFactory();
		JavaType arrayType = typeFactory.constructCollectionType(List.class, Pipeline.class);
		try {
			actualPipelines = mapper.readValue(pipelines.toString(), arrayType);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		var newCamera = actualPipelines != null ? new Camera(camName, camFOV, actualPipelines, videoModeIndex, divisor) : new Camera(camName, camFOV, videoModeIndex, divisor);
		newCamera.setNickname(camNickname != null ? camNickname : "");
		return newCamera;
	}
}
