package org.photonvision.common.vision.pipeline;

@SuppressWarnings("rawtypes")
public enum PipelineType {
    Calib3d(-2, Calibration3dPipeline.class),
    DriverMode(-1, DriverModePipeline.class),
    Reflective(0, ReflectivePipeline.class),
    ColoredShape(0, ColoredShapePipeline.class);

    public final int baseIndex;
    public final Class clazz;

    PipelineType(int baseIndex, Class clazz) {
        this.baseIndex = baseIndex;
        this.clazz = clazz;
    }
}
