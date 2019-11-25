package com.chameleonvision.vision.camera;

import com.chameleonvision.vision.enums.StreamDivisor;
import com.chameleonvision.web.ServerHandler;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.cameraserver.CameraServer;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class CameraStreamer {
    private final CameraCapture cameraCapture;
    private final String name;
    private StreamDivisor divisor = StreamDivisor.NONE;
    private CvSource cvSource;
    private final Object streamBufferLock = new Object();
    private Mat streamBuffer = new Mat();

    public CameraStreamer(CameraCapture cameraCapture, String name) {
        this.cameraCapture = cameraCapture;
        this.name = name;
        this.cvSource = CameraServer.getInstance().putVideo(name,
                cameraCapture.getProperties().getStaticProperties().imageWidth / divisor.value,
                cameraCapture.getProperties().getStaticProperties().imageHeight / divisor.value);
        setDivisor(divisor, false);
    }

    public void setDivisor(StreamDivisor newDivisor, boolean updateUI) {
        this.divisor = newDivisor;
        var camValues = cameraCapture.getProperties();
        var newWidth = camValues.getStaticProperties().imageWidth / newDivisor.value;
        var newHeight = camValues.getStaticProperties().imageHeight / newDivisor.value;
        synchronized (streamBufferLock) {
            this.streamBuffer = new Mat(newWidth, newHeight, CvType.CV_8UC3);
            this.cvSource = CameraServer.getInstance().putVideo(this.name,
                    cameraCapture.getProperties().getStaticProperties().imageWidth / divisor.value,
                    cameraCapture.getProperties().getStaticProperties().imageHeight / divisor.value);
        }
        if (updateUI) {
            ServerHandler.sendFullSettings();
        }
    }

    public StreamDivisor getDivisor() {
        return divisor;
    }

    public void setNewVideoMode(VideoMode newVideoMode) {
        // Trick to update cvSource and streamBuffer to the new resolution
        // Must change the cameraProcess resolution first
        setDivisor(divisor, true);
    }

    public int getStreamPort() {
        var s = (MjpegServer) CameraServer.getInstance().getServer("serve_" + name);
        return s.getPort();
    }

    public void runStream(Mat image) {
        synchronized (streamBufferLock) {
            streamBuffer = image;
        }
//        if (divisor.value != 1) {
//            var camVal = cameraProcess.getProperties().staticProperties;
//            var newWidth = camVal.imageWidth / divisor.value;
//            var newHeight = camVal.imageHeight / divisor.value;
//            Size newSize = new Size(newWidth, newHeight);
//            Imgproc.resize(streamBuffer, streamBuffer, newSize);
//        }
        cvSource.putFrame(streamBuffer);
    }
}
