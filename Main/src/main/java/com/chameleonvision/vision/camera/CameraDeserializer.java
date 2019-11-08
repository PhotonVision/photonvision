package com.chameleonvision.vision.camera;

import com.chameleonvision.vision.Pipeline;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CameraDeserializer implements JsonDeserializer<USBCamera> {
	@Override
	public USBCamera deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
		try {
			var jsonObj = jsonElement.getAsJsonObject();
			var camFOV = jsonObj.get("FOV").getAsDouble();
			var camName = jsonObj.get("name").getAsString();
			var camNickname = jsonObj.get("nickname").getAsString();
			var videoModeIndex = jsonObj.get("resolution").getAsInt();

			// new for 2.0
			var isDriverObj = jsonObj.get("isDriver");
			var driverExposureObj = jsonObj.get("driverExposure");
			var driverBrightnessObj = jsonObj.get("driverBrightness");
			var divisorObj = jsonObj.get("streamDivisor");

			// always null-check new features
			boolean isDriver = isDriverObj != null && isDriverObj.getAsBoolean();
			int driverExposure = driverExposureObj == null ? USBCamera.DEFAULT_EXPOSURE : driverExposureObj.getAsInt();
			int driverBrightness = driverBrightnessObj == null ? USBCamera.DEFAULT_BRIGHTNESS : driverBrightnessObj.getAsInt();
			StreamDivisor divisor = divisorObj == null ? StreamDivisor.NONE : StreamDivisor.values()[divisorObj.getAsInt()];

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

			var newCamera = actualPipelines != null ? new USBCamera(camName, camFOV, actualPipelines, videoModeIndex, divisor, isDriver) : new USBCamera(camName, camFOV, videoModeIndex, divisor, isDriver);
			newCamera.setNickname(camNickname != null ? camNickname : "");
			newCamera.setDriverExposure(driverExposure);
			newCamera.setDriverBrightness(driverBrightness);
			return newCamera;
		}
		catch (NullPointerException e)
		{
			System.err.println("Error while reading json, value doesn't exist!");
			System.err.println("Try to delete the camera settings in settings/cameras/YOURCAMERA.json");
			e.printStackTrace();
			return null;
		}
	}
}
