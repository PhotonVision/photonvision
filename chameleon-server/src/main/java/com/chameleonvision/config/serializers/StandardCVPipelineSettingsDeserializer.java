package com.chameleonvision.config.serializers;

import com.chameleonvision.vision.enums.*;
import com.chameleonvision.vision.pipeline.impl.StandardCVPipelineSettings;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;

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

        pipeline.hue = getNumberList("hue", pipeline.hue);
        pipeline.saturation = getNumberList("saturation", pipeline.saturation);
        pipeline.value = getNumberList("value", pipeline.value);

        pipeline.erode = getBoolean("erode", pipeline.erode);
        pipeline.dilate = getBoolean("dilate", pipeline.dilate);

        pipeline.area = getNumberList("area", pipeline.area);
        pipeline.ratio = getNumberList("ratio", pipeline.ratio);
        pipeline.extent = getNumberList("extent", pipeline.extent);

        pipeline.speckle = getInt("speckle", (Integer) pipeline.speckle);

        pipeline.isBinary = getBoolean("isBinary", pipeline.isBinary);

        pipeline.sortMode = getEnum("sortMode", SortMode.class, pipeline.sortMode);
        pipeline.targetRegion = getEnum("targetRegion", TargetRegion.class, pipeline.targetRegion);
        pipeline.targetOrientation = getEnum("targetOrientation", TargetOrientation.class, pipeline.targetOrientation);

        pipeline.multiple = getBoolean("multiple", pipeline.multiple);

        pipeline.targetGroup = getEnum("targetGroup", TargetGroup.class, pipeline.targetGroup);
        pipeline.targetIntersection = getEnum("targetIntersection", TargetIntersection.class, pipeline.targetIntersection);

        pipeline.point = getNumberList("point", pipeline.point);
        
        pipeline.calibrationMode = getEnum("calibrationMode", CalibrationMode.class, pipeline.calibrationMode);
        
        pipeline.dualTargetCalibrationM = getDouble("dualTargetCalibrationM", pipeline.dualTargetCalibrationM);
        pipeline.dualTargetCalibrationB = getDouble("dualTargetCalibrationB", pipeline.dualTargetCalibrationB);

        pipeline.is3D = getBoolean("is3D", pipeline.is3D);
        pipeline.targetCornerMat = getMatOfPoint3f("targetCornerMat", pipeline.targetCornerMat);

        return pipeline;
    }
}
