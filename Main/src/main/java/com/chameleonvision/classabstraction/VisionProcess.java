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

    // shitty stuff
    private volatile Mat lastCameraFrame = new Mat();
    private volatile CVPipelineResult lastPipelineResult;

    public VisionProcess(CameraProcess cameraProcess, String name) {
        this.cameraProcess = cameraProcess;

        pipelines.add(new DriverVisionPipeline(() -> driverVisionSettings));
        setPipeline(pipelines.get(0));

        // Thread to grab frames from the camera
        // TODO: fix video modes!!!
        this.cameraRunnable = new CameraFrameRunnable(cameraProcess.getProperties().videoModes.get(0).fps);

        lastPipelineResult = new DriverVisionPipeline.DriverPipelineResult(
                null, cameraRunnable.getFrame(new Mat()), 0
        );

        // Thread to put frames on the dashboard
        this.cameraStreamer = new CameraStreamer(cameraProcess, name);
        this.streamRunnable = new CameraStreamerRunnable(1000L/32, cameraStreamer);

        // Thread to process vision data
        this.visionRunnable = new VisionProcessRunnable();
    }

    public void start() {
        System.out.println("Starting camera thread.");
        new Thread(cameraRunnable).start();
        while (cameraRunnable.cameraFrame == null) {
            try {
                if (cameraRunnable.cameraFrame.cols() > 0) break;
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
        System.out.println("Starting vision thread.");
        new Thread(visionRunnable).start();
        System.out.println("Starting stream thread.");
        new Thread(streamRunnable).start();
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
        private Mat cameraFrame;
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
                if (camData.getLeft().cols() > 0) {
//                    System.out.println("grabbing frame");
//                    synchronized (frameLock) {
//                        cameraFrame = camData.getLeft();
//                    }
                    timestampMicros = camData.getRight();
                    camData.getLeft().copyTo(lastCameraFrame);
                }
            }
        }

        public Mat getFrame(Mat dst) {
            if (cameraFrame != null) {
                dst = cameraFrame;
            } else {
                System.out.println("no frame");
            }
            return dst;
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
                lastCameraFrame.copyTo(streamBuffer); // = //cameraRunnable.getFrame(streamBuffer);
                if (streamBuffer.cols() > 0 && streamBuffer.rows() > 0) {
                    result = currentPipeline.runPipeline(streamBuffer);
                    lastPipelineResult = result;
                } else {
//                    System.err.println("Bad streambuffer mat");
                }
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
            CVPipelineResult latestResult = lastPipelineResult; //visionRunnable.result;
            if (latestResult != null) {
                Mat toStreamMat = visionRunnable.result.outputMat;
                toStreamMat.copyTo(streamBuffer);
                streamer.runStream(streamBuffer);
//                if (toStreamMat != null && toStreamMat.cols() > 0) {
//                } else {
//                    System.out.println("fuuuuck");
//                }
            }
        }
    }
}
