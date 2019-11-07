package com.chameleonvision.classabstraction.pipeline;

import org.opencv.core.Mat;

import java.util.List;

public abstract class CVPipelineResult<T> {
    List<T> targets;
    boolean hasTarget;
    Mat outputMat;
}
