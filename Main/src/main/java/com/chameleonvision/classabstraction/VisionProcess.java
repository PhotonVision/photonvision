package com.chameleonvision.classabstraction;

import com.chameleonvision.classabstraction.camera.CameraProcess;
import com.chameleonvision.classabstraction.camera.CameraStreamer;
import com.chameleonvision.classabstraction.pipeline.CVPipeline;
import com.chameleonvision.classabstraction.pipeline.CVPipelineResult;
import com.chameleonvision.classabstraction.pipeline.CVPipelineSettings;
import com.chameleonvision.classabstraction.pipeline.DriverVisionPipeline;
import edu.wpi.cscore.VideoMode;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

public class VisionProcess {

    private final CameraProcess cameraProcess;
    private final List<CVPipeline> pipelines = new ArrayList<>();
    private final CameraFrameRunnable cameraRunnable;
    private final CameraStramerRunnable streamRunnable;
    private final VisionProcessRunnable visionRunnable;
    private final CameraStreamer cameraStreamer;
    private CVPipeline currentPipeline;

    private final CVPipelineSettings driverVisionSettings = new CVPipelineSettings();

    public VisionProcess(CameraProcess cameraProcess, String name, Long streamTimeMs, Long cameraUpdateTimeMs) {
        this.cameraProcess = cameraProcess;

        pipelines.add(new DriverVisionPipeline(() -> driverVisionSettings));
        setPipeline(pipelines.get(0));

        // Thread to grab frames from the camera
        this.cameraRunnable = new CameraFrameRunnable(cameraUpdateTimeMs) ;
        new Thread(cameraRunnable).start();

        // Thread to put frames on the dashboard
        this.cameraStreamer = new CameraStreamer(cameraProcess, name);
        this.streamRunnable = new CameraStramerRunnable(streamTimeMs, cameraStreamer);
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
         * at a specified loopTime
         * @param loopTimeMs how often to grab frames at in ms
         */
        CameraFrameRunnable(Long loopTimeMs) {
            super(loopTimeMs);
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

    private class CameraStramerRunnable extends LoopingRunnable {

        private final CameraStreamer streamer;
        private Mat streamBuffer = new Mat();

        private CameraStramerRunnable(Long loopTimeMs, CameraStreamer streamer) {
            super(loopTimeMs);
            this.streamer = streamer;
        }

        @Override
        void process() {
            streamer.runStream(cameraRunnable.getFrame(streamBuffer));
        }
    }

    /**
     * A thread that tries to run at a specified loop time
     */
    private static abstract class LoopingRunnable implements Runnable {
        private final Long loopTimeMs;

        abstract void process();

        private LoopingRunnable(Long loopTimeMs) {
            this.loopTimeMs = loopTimeMs;
        }

        @Override
        public void run() {
            while(!Thread.interrupted()) {
                var now = System.currentTimeMillis();

                // Do the thing
                process();

                // sleep for the remaining time
                var timeElapsed = System.currentTimeMillis() - now;
                var delta = loopTimeMs - timeElapsed;
                if(delta > 0.0) {
                    try {
                        Thread.sleep(delta, 0);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

}
