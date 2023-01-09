/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.videoStream;

import edu.wpi.first.cscore.CameraServerJNI;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.consumer.MJPGFrameConsumer;
import org.photonvision.vision.opencv.CVMat;

public class SocketVideoStream implements Consumer<CVMat> {
    int portID = 0; // Align with cscore's port for unique identification of stream
    MatOfByte jpegBytes = null;

    // Gets set to true when another class reads out valid jpeg bytes at least once
    // Set back to false when another frame is freshly converted
    // Should eliminate synchronization issues of differeing rates of putting frames in
    // and taking them back out
    boolean frameWasConsumed = false;

    // Synclock around manipulating the jpeg bytes from multiple threads
    Lock jpegBytesLock = new ReentrantLock();
    private int userCount = 0;

    // FPS-limited MJPEG sender
    private final double FPS_MAX = 30.0;
    private final long minFramePeriodNanos = Math.round(1000000000.0 / FPS_MAX);
    private long nextFrameSendTime = MathUtils.wpiNanoTime() + minFramePeriodNanos;
    MJPGFrameConsumer oldSchoolServer;

    public SocketVideoStream(int portID) {
        this.portID = portID;
        oldSchoolServer =
                new MJPGFrameConsumer(
                        CameraServerJNI.getHostname() + "_Port_" + Integer.toString(portID) + "_MJPEG_Server",
                        portID);
    }

    @Override
    public void accept(CVMat image) {
        if (userCount > 0) {
            if (jpegBytesLock
                    .tryLock()) { // we assume frames are coming in frequently. Just skip this frame if we're
                // locked doing something else.
                try {
                    // Does a single-shot frame recieve and convert to JPEG for efficency
                    // Will not capture/convert again until convertNextFrame() is called
                    if (image != null && !image.getMat().empty() && jpegBytes == null) {
                        frameWasConsumed = false;
                        jpegBytes = new MatOfByte();
                        Imgcodecs.imencode(
                                ".jpg",
                                image.getMat(),
                                jpegBytes,
                                new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 75));
                    }
                } finally {
                    jpegBytesLock.unlock();
                }
            }
        }

        // Send the frame in an FPS-limited fashion
        var now = MathUtils.wpiNanoTime();
        if (now > nextFrameSendTime) {
            oldSchoolServer.accept(image);
            nextFrameSendTime = now + minFramePeriodNanos;
        }
    }

    public ByteBuffer getJPEGByteBuffer() {
        ByteBuffer sendStr = null;
        jpegBytesLock.lock();
        if (jpegBytes != null) {
            sendStr = ByteBuffer.wrap(jpegBytes.toArray());
        }
        jpegBytesLock.unlock();
        return sendStr;
    }

    public void convertNextFrame() {
        jpegBytesLock.lock();
        if (jpegBytes != null) {
            jpegBytes.release();
            jpegBytes = null;
        }
        jpegBytesLock.unlock();
    }

    public void addUser() {
        userCount++;
    }

    public void removeUser() {
        userCount--;
    }
}
