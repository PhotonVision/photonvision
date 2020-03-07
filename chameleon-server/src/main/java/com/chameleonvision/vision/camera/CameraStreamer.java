package com.chameleonvision.vision.camera;

import com.chameleonvision.vision.enums.StreamDivisor;
import com.chameleonvision.web.SocketHandler;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.cameraserver.CameraServer;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class CameraStreamer {
    private final CameraCapture cameraCapture;
    private final String name;
    private StreamDivisor divisor;
    private CvSource cvSource;
    private final Object streamBufferLock = new Object();
    private Mat streamBuffer = new Mat();
    private Size size;

    public CameraStreamer(CameraCapture cameraCapture, String name,StreamDivisor div) {
        this.divisor = div;
        this.cameraCapture = cameraCapture;
        this.name = name;
        this.cvSource = CameraServer.getInstance().putVideo(name,
                cameraCapture.getProperties().getStaticProperties().imageWidth / divisor.value,
                cameraCapture.getProperties().getStaticProperties().imageHeight / divisor.value);
        //noinspection IntegerDivisionInFloatingPointContext
        this.size = new Size(
                cameraCapture.getProperties().getStaticProperties().imageWidth / divisor.value,
                cameraCapture.getProperties().getStaticProperties().imageHeight / divisor.value
        );
        setDivisor(divisor, false);
    }

    public void setDivisor(StreamDivisor newDivisor, boolean updateUI) {
        this.divisor = newDivisor;
        var camValues = cameraCapture.getProperties();
        var newWidth = camValues.getStaticProperties().imageWidth / newDivisor.value;
        var newHeight = camValues.getStaticProperties().imageHeight / newDivisor.value;
        this.size = new Size(newWidth, newHeight);
        synchronized (streamBufferLock) {
            this.streamBuffer = new Mat(newWidth, newHeight, CvType.CV_8UC3);
            VideoMode oldVideoMode = cvSource.getVideoMode();
            cvSource.setVideoMode(new VideoMode(oldVideoMode.pixelFormat,
                    cameraCapture.getProperties().getStaticProperties().imageWidth / divisor.value,
                    cameraCapture.getProperties().getStaticProperties().imageHeight / divisor.value,
                    oldVideoMode.fps));
        }
        if (updateUI) {
            SocketHandler.sendFullSettings();
        }

    }

    public StreamDivisor getDivisor() {
        return divisor;
    }

    public void recalculateDivision() {
        setDivisor(this.divisor, false);
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
            image.copyTo(streamBuffer);
        }

        if (divisor.value != 1) {
//            var camVal = cameraProcess.getProperties().staticProperties;
//            var newWidth = camVal.imageWidth / divisor.value;
//            var newHeight = camVal.imageHeight / divisor.value;
//            Size newSize = new Size(newWidth, newHeight);
             Imgproc.resize(streamBuffer, streamBuffer, this.size);
        }

        var sourceVideoMode = cvSource.getVideoMode();
        var imageSize = streamBuffer.size();
        if(sourceVideoMode.width != (int) imageSize.width || sourceVideoMode.height != (int) imageSize.height) {
            synchronized (streamBufferLock) {
                cvSource.setVideoMode(new VideoMode(sourceVideoMode.pixelFormat,
                        (int)imageSize.width,
                        (int) imageSize.height,
                        sourceVideoMode.fps));
            }
        }

        cvSource.putFrame(streamBuffer);
    }
}
