package com.chameleonvision.classabstraction;

import com.chameleonvision.classabstraction.camera.CameraProcess;
import com.chameleonvision.classabstraction.camera.CameraStreamer;
import com.chameleonvision.classabstraction.pipeline.CVPipeline;
import com.chameleonvision.classabstraction.pipeline.CVPipelineResult;
import com.chameleonvision.classabstraction.pipeline.CVPipelineSettings;
import com.chameleonvision.classabstraction.pipeline.DriverVisionPipeline;
import com.chameleonvision.classabstraction.util.LoopingRunnable;
import edu.wpi.cscore.VideoMode;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

public class VisionProcess {

    private final CameraProcess cameraProcess;
    private final List<CVPipeline> pipelines = new ArrayList<>();
    private final CameraFrameRunnable cameraRunnable;
    private final CameraStreamerRunnable streamRunnable;
    private final VisionProcessRunnable visionRunnable;
    private final CameraStreamer cameraStreamer;
    private CVPipeline currentPipeline;

    private final CVPipelineSettings driverVisionSettings = new CVPipelineSettings();

    public VisionProcess(CameraProcess cameraProcess, String name, Long streamTimeMs, Long cameraUpdateTimeMs) {
        this.cameraProcess = cameraProcess;

        pipelines.add(new DriverVisionPipeline(() -> driverVisionSettings));
        setPipeline(pipelines.get(0));

        // Thread to grab frames from the camera
        // TODO: fix video modes!!!
        this.cameraRunnable = new CameraFrameRunnable(cameraProcess.getProperties().videoModes.get(0).fps);
        new Thread(cameraRunnable).start();

        // Thread to put frames on the dashboard
        this.cameraStreamer = new CameraStreamer(cameraProcess, name);
        this.streamRunnable = new CameraStreamerRunnable(streamTimeMs, cameraStreamer);
        new Thread(streamRunnable).start();

        // Thread to process vision data
        this.visionRunnable = new VisionProcessRunnable();
        new Thread(visionRunnable).start();

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

    public void setVideoMode(VideoMode newMode) {
        cameraProcess.setVideoMode(newMode);
        cameraStreamer.setNewVideoMode(newMode);
    }

    public CVPipeline getCurrentPipeline() {
        return currentPipeline;
    }

    /**
     * CameraFrameRunnable grabs images from the cameraProcess
     * at a specified loopTime
     */
    protected class CameraFrameRunnable extends LoopingRunnable {
        private Mat cameraFrame = new Mat();
        private long timestampMicros;

        private final Object frameLock = new Object();

        /**
         * CameraFrameRunnable grabs images from the cameraProcess
         * at a specified framerate
         * @param cameraFPS FPS of camera
         */
        CameraFrameRunnable(int cameraFPS) {
            // add 2 FPS to allow for a bit of overhead
            // TODO: test the affect of this
            super(1000L/(cameraFPS + 2));
        }

        @Override
        public void process() {
            while(!Thread.interrupted()) {

                // Grab camera frames
                var camData = cameraProcess.getFrame();
                synchronized (frameLock) {
                    cameraFrame = camData.getLeft();
                }
                timestampMicros = camData.getRight();
            }
        }

        public Mat getFrame(Mat dst) {
            synchronized (frameLock) {
                cameraFrame.copyTo(dst);
                return dst;
            }
        }

        public long getTimestampMicros() {
            return timestampMicros;
        }
    }

    /**
     * VisionProcessRunnable will process images as quickly as possible
     */
    private class VisionProcessRunnable implements Runnable {

        private CVPipelineResult result;
        private Mat streamBuffer = new Mat();

        @Override
        public void run() {
            while(!Thread.interrupted()) {
                result = currentPipeline.runPipeline(cameraRunnable.getFrame(streamBuffer));
                // TODO do something with the result
            }
        }
    }

    private class CameraStreamerRunnable extends LoopingRunnable {

        private final CameraStreamer streamer;
        private Mat streamBuffer = new Mat();

        private CameraStreamerRunnable(Long loopTimeMs, CameraStreamer streamer) {
            super(loopTimeMs);
            this.streamer = streamer;
        }

        @Override
        protected void process() {
            streamer.runStream(cameraRunnable.getFrame(streamBuffer));
        }
    }
}
