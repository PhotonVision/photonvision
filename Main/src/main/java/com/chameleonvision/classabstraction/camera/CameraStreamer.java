package com.chameleonvision.classabstraction.camera;

import com.chameleonvision.vision.camera.CameraManager;
import com.chameleonvision.vision.camera.StreamDivisor;
import com.chameleonvision.web.ServerHandler;
import edu.wpi.cscore.CvSource;
import edu.wpi.first.cameraserver.CameraServer;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class CameraStreamer {

    private final CameraProcess cameraProcess;
    private final String name;
    private StreamDivisor divisor = StreamDivisor.NONE;
    private CvSource cvSource;
    private final Object streamBufferLock = new Object();
    private Mat streamBuffer = new Mat();

    public CameraStreamer(CameraProcess cameraProcess, String name) {
        this.cameraProcess = cameraProcess;
        this.name = name;
        this.cvSource = CameraServer.getInstance().putVideo(name,
                cameraProcess.getProperties().staticProperties.imageWidth / divisor.value,
                cameraProcess.getProperties().staticProperties.imageHeight / divisor.value);
    }


    public void setDivisor(StreamDivisor newDivisor) {
        this.divisor = newDivisor;
        var camValues = cameraProcess.getProperties();
        var newWidth = camValues.staticProperties.imageWidth / newDivisor.value;
        var newHeight = camValues.staticProperties.imageHeight / newDivisor.value;
        synchronized (streamBufferLock) {
            this.streamBuffer = new Mat(newWidth, newHeight, CvType.CV_8UC3);
            this.cvSource = CameraServer.getInstance().putVideo(this.name,
                    cameraProcess.getProperties().staticProperties.imageWidth / divisor.value,
                    cameraProcess.getProperties().staticProperties.imageHeight / divisor.value);
        }
        ServerHandler.sendFullSettings();
    }

    public void runStream() {
        var newFrame = cameraProcess.getFrame(streamBuffer);
        var image = newFrame.getLeft();
        if (divisor.value != 1) {
            var camVal = cameraProcess.getProperties().staticProperties;
            var newWidth = camVal.imageWidth / divisor.value;
            var newHeight = camVal.imageHeight / divisor.value;
            Size newSize = new Size(newWidth, newHeight);
            Imgproc.resize(image, image, newSize);
        }
        cvSource.putFrame(image);
    }

}
