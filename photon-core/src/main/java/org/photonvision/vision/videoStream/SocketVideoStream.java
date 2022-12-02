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

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.consumer.MJPGFrameConsumer;

public class SocketVideoStream implements Consumer<Frame> {
    int portID = 0; // Align with cscore's port for unique identification of stream
    MatOfByte jpegBytes = null;

    // Gets set to true when another class reads out valid jpeg bytes at least once
    // Set back to false when another frame is freshly converted
    // Should eliminate synchronization issues of differeing rates of putting frames in
    // and taking them back out
    boolean frameWasConsumed = false;

    // Synclock around manipulating the jpeg bytes from multiple threads
    Lock jpegBytesLock = new ReentrantLock();

    MJPGFrameConsumer oldSchoolServer;

    private int userCount = 0;

    public SocketVideoStream(int portID) {
        this.portID = portID;
        oldSchoolServer =
                new MJPGFrameConsumer("Port_" + Integer.toString(portID) + "_MJPEG_Server", portID);
    }

    @Override
    public void accept(Frame frame) {
        if (userCount > 0) {
            if (jpegBytesLock
                    .tryLock()) { // we assume frames are coming in frequently. Just skip this frame if we're
                // locked doing something else.
                try {
                    // Does a single-shot frame recieve and convert to JPEG for efficency
                    // Will not capture/convert again until convertNextFrame() is called
                    if (frame != null && !frame.image.getMat().empty() && jpegBytes == null) {
                        frameWasConsumed = false;
                        jpegBytes = new MatOfByte();
                        Imgcodecs.imencode(
                                ".jpg",
                                frame.image.getMat(),
                                jpegBytes,
                                new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 75));
                    }
                } finally {
                    jpegBytesLock.unlock();
                }
            }
        }
        oldSchoolServer.accept(frame);
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
