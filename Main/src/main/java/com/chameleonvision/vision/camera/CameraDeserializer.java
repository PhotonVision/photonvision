package com.chameleonvision.vision.camera;

import com.chameleonvision.vision.Pipeline;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;

public class CameraDeserializer implements JsonDeserializer<Camera> {
	@Override
	public Camera deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
		var jsonObj = jsonElement.getAsJsonObject();
		var camFOV = jsonObj.get("FOV").getAsDouble();
		var camName = jsonObj.get("name").getAsString();
		var videoModeIndex = jsonObj.get("resolution").getAsInt();
		var divisor = StreamDivisor.values()[jsonObj.get("streamDivisor").getAsInt()];


		var pipelines = jsonObj.get("pipelines");
		HashMap<Integer, Pipeline> actualPipelines = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		TypeFactory typeFactory = mapper.getTypeFactory();
		MapType mapType = typeFactory.constructMapType(HashMap.class, Integer.class, Pipeline.class);
		try {
			actualPipelines = mapper.readValue(pipelines.toString(), mapType);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		return actualPipelines != null ? new Camera(camName, camFOV, actualPipelines, videoModeIndex,divisor) : new Camera(camName, camFOV, videoModeIndex, divisor);
	}
}
