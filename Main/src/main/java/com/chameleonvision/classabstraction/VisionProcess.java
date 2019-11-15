package com.chameleonvision.classabstraction;

import com.chameleonvision.classabstraction.camera.CameraProcess;
import com.chameleonvision.classabstraction.pipeline.CVPipeline;
import com.chameleonvision.classabstraction.pipeline.CVPipelineResult;
import com.chameleonvision.classabstraction.pipeline.CVPipelineSettings;
import com.chameleonvision.classabstraction.pipeline.DriverVisionPipeline;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

public class VisionProcess {

    private final CameraProcess cameraProcess;
    private final List<CVPipeline> pipelines = new ArrayList<>();
    private CVPipeline currentPipeline;

    private final CameraFrameRunnable cameraFrameRunnable;

    private final CVPipelineSettings driverVisionSettings = new CVPipelineSettings();

    public VisionProcess(CameraProcess cameraProcess) {
        this.cameraProcess = cameraProcess;

        pipelines.add(new DriverVisionPipeline(() -> driverVisionSettings));
        setPipeline(pipelines.get(0));

        cameraFrameRunnable = new CameraFrameRunnable();
    }

    public void setPipeline(int pipelineIndex) {
        CVPipeline newPipeline = pipelines.get(pipelineIndex);
        if (newPipeline != null) {
            setPipeline(newPipeline);
        }
    }

    public void setPipeline(CVPipeline pipeline) {
        currentPipeline = pipeline;
        currentPipeline.initPipeline(cameraProcess);
    }

    public CVPipeline getCurrentPipeline() {
        return currentPipeline;
    }

    protected class CameraFrameRunnable implements Runnable {
        private Mat cameraFrame = new Mat();
        private long timestampMicros;

        private final Object frameLock = new Object();

        @Override
        public void run() {
            while(Thread.interrupted()) {
                var camData = cameraProcess.getFrame();
                synchronized (frameLock) {
                    cameraFrame = camData.getLeft();
                }
                timestampMicros = camData.getRight();
            }
        }

        public Mat getFrame() {
            return cameraFrame;
        }

        public long getTimestampMicros() {
            return timestampMicros;
        }
    }

    private class VisionThread implements Runnable {

        private CVPipelineResult result;

        @Override
        public void run() {
            while(!Thread.interrupted()) {
                result = currentPipeline.runPipeline(cameraFrameRunnable.getFrame());
            }
        }
    }
}
