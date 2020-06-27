package org.photonvision._2.vision.pipeline;

import java.util.List;
import org.opencv.core.Mat;

public abstract class CVPipelineResult<T> {
    public final List<T> targets;
    public final boolean hasTarget;
    public final Mat outputMat = new Mat();
    public final long processTime;
    public long imageTimestamp = 0;

    public CVPipelineResult(List<T> targets, Mat outputMat, long processTime) {
        this.targets = targets;
        hasTarget = targets != null && !targets.isEmpty();
        //        this.outputMat = outputMat;
        outputMat.copyTo(this.outputMat);
        outputMat.release();
        this.processTime = processTime;
    }

    public void setTimestamp(long timestamp) {
        imageTimestamp = timestamp;
    }
}
