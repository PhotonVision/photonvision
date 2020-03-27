package com.chameleonvision._2.config.serializers;

import com.chameleonvision._2.vision.enums.*;
import com.chameleonvision._2.vision.pipeline.impl.StandardCVPipelineSettings;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;

import java.io.IOException;

public class StandardCVPipelineSettingsDeserializer extends BaseDeserializer<StandardCVPipelineSettings> {
    public StandardCVPipelineSettingsDeserializer() {
        this(null);
    }

    private StandardCVPipelineSettingsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public StandardCVPipelineSettings deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        // set BaseDeserializer parser reference.
        baseNode = jsonParser.getCodec().readTree(jsonParser);

        StandardCVPipelineSettings pipeline = new StandardCVPipelineSettings();

        pipeline.index = getInt("index", pipeline.index);

        pipeline.flipMode = getEnum("flipMode", ImageFlipMode.class, pipeline.flipMode);
        pipeline.rotationMode = getEnum("rotationMode", ImageRotationMode.class, pipeline.rotationMode);

        pipeline.nickname = getString("nickname", pipeline.nickname);

        pipeline.exposure = getDouble("exposure", pipeline.exposure);
        pipeline.brightness = getDouble("brightness", pipeline.brightness);
        pipeline.gain = getDouble("gain", pipeline.gain);

        pipeline.videoModeIndex = getInt("videoModeIndex", pipeline.videoModeIndex);

        pipeline.streamDivisor = getEnum("streamDivisor", StreamDivisor.class, pipeline.streamDivisor);

        pipeline.hue = getNumberCouple("hue", pipeline.hue);
        pipeline.saturation = getNumberCouple("saturation", pipeline.saturation);
        pipeline.value = getNumberCouple("value", pipeline.value);

        pipeline.erode = getBoolean("erode", pipeline.erode);
        pipeline.dilate = getBoolean("dilate", pipeline.dilate);

        pipeline.area = getNumberCouple("area", pipeline.area);
        pipeline.ratio = getNumberCouple("ratio", pipeline.ratio);
        pipeline.extent = getNumberCouple("extent", pipeline.extent);

        pipeline.speckle = getInt("speckle", (Integer) pipeline.speckle);

        pipeline.isBinary = getBoolean("isBinary", pipeline.isBinary);

        pipeline.sortMode = getEnum("sortMode", SortMode.class, pipeline.sortMode);
        pipeline.targetRegion = getEnum("targetRegion", TargetRegion.class, pipeline.targetRegion);
        pipeline.targetOrientation = getEnum("targetOrientation", TargetOrientation.class, pipeline.targetOrientation);

        pipeline.multiple = getBoolean("multiple", pipeline.multiple);

        pipeline.targetGroup = getEnum("targetGroup", TargetGroup.class, pipeline.targetGroup);
        pipeline.targetIntersection = getEnum("targetIntersection", TargetIntersection.class, pipeline.targetIntersection);

        pipeline.point = getNumberCouple("point", pipeline.point);
        
        pipeline.calibrationMode = getEnum("calibrationMode", CalibrationMode.class, pipeline.calibrationMode);
        
        pipeline.dualTargetCalibrationM = getDouble("dualTargetCalibrationM", pipeline.dualTargetCalibrationM);
        pipeline.dualTargetCalibrationB = getDouble("dualTargetCalibrationB", pipeline.dualTargetCalibrationB);

        pipeline.is3D = getBoolean("is3D", pipeline.is3D);
        pipeline.targetCornerMat = getMatOfPoint3f("targetCornerMat", pipeline.targetCornerMat);
        pipeline.accuracy = getDouble("accuracy", pipeline.accuracy.doubleValue());

        return pipeline;
    }
}
