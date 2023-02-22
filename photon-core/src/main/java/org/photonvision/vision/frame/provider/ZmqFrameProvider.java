package org.photonvision.vision.frame.provider;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;
import org.zeromq.ZMQ;
import org.zeromq.SocketType;
import org.zeromq.ZContext;

/**
 * A {@link FrameProvider} that will read and provide an image from a {@link java.nio.file.Path
 * path}.
 */
public class ZmqFrameProvider extends CpuImageProcessor {
    public static final int MAX_FPS = 60;
    
    private static final Logger logger = new Logger(ZmqFrameProvider.class, LogGroup.Camera);

    private final ZContext context;
    private final ZMQ.Socket socket;
    private final String address;
    private final String topic;
    private final int millisDelay;
    private final FrameStaticProperties properties;

    private long lastGetMillis = System.currentTimeMillis();

    public ZmqFrameProvider(String address, String topic, double fov, int maxFPS) {
        this(address, topic, fov, maxFPS, null);
    }

    public ZmqFrameProvider(String address, String topic, double fov, CameraCalibrationCoefficients calibration) {
        this(address, topic, fov, MAX_FPS, calibration);
    }

    public ZmqFrameProvider(String address, String topic, double fov, int maxFPS, CameraCalibrationCoefficients calibration) {
        this.address = address;
        this.topic = topic;
        this.millisDelay = 1000 / maxFPS;

        this.context = new ZContext();
        this.socket = context.createSocket(SocketType.SUB);
        this.socket.connect(this.address);
        this.socket.subscribe(this.topic);
        
        var sampleFrame = this.receiveFrame().getMat();
        this.properties = new FrameStaticProperties(sampleFrame.width(), sampleFrame.height(), fov, calibration);
    }

    private CVMat receiveFrame() {
        // We expect to receive a 6-part message.
        //   0: topic (ignored)
        //   1: height (4 bytes, little-endian)
        //   2: width (4 bytes, little-endian)
        //   3: depth (1 byte)
        //   4: channels (1 byte)
        //   5: image buffer (byte array)
        int imageHeight = 0, imageWidth = 0, imageDepth = 0, imageChannels = 0;
        byte[] imageData = null;
        byte[] message = this.socket.recv();
        int messageIndex = 0;
        while (true) {
            switch (messageIndex) {
                case 0:
                    // Ignore topic.
                    break;
                case 1:
                    imageHeight = ByteBuffer.wrap(message).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    break;
                case 2:
                    imageWidth = ByteBuffer.wrap(message).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    break;
                case 3:
                    imageDepth = message[0];
                    break;
                case 4:
                    imageChannels = message[0];
                    break;
                case 5:
                    imageData = message;
                    break;
                default:
                    // Ignore any extra message parts.
                    break;
            }

            ++messageIndex;
            if (this.socket.hasReceiveMore()) {
                // Receive the next part of the message.
                message = this.socket.recv();
            } else {
                // Receive another message immediately if possible. This prevents the consumer from falling
                // behind by skipping frames if they're being produced faster than they're being consumed.
                message = this.socket.recv(ZMQ.NOBLOCK);
                messageIndex = 0;
                if (message == null) {
                    break;
                }
            }
        }

        if (imageData == null) {
            logger.error("Skipping frame! Received ZMQ message with only " + messageIndex + " parts.");
            return new CVMat();
        }

        var matType = CvType.makeType(imageDepth, imageChannels);
        var mat = new Mat(imageHeight, imageWidth, matType);
        mat.put(0, 0, imageData);
        return new CVMat(mat);
    }

    @Override
    public CapturedFrame getInputMat() {
        var millis = System.currentTimeMillis() - this.lastGetMillis;

        if (millis < this.millisDelay) {
            // Sleep to keep FPS below the cap.
            try {
                Thread.sleep(this.millisDelay - millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        var frame = this.receiveFrame();
        this.lastGetMillis = System.currentTimeMillis();
        return new CapturedFrame(frame, this.properties, MathUtils.wpiNanoTime());
    }

    @Override
    public String getName() {
        return "ZmqFrameProvider - " + this.address + "/" + this.topic;
    }
}
