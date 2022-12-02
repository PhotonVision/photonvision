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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.consumer.MJPGFrameConsumer;

public class SocketVideoStream implements Consumer<Frame> {
    int portID = 0; // Align with cscore's port for unique identification of stream

    ConcurrentLinkedQueue<Frame> frameQueue = new ConcurrentLinkedQueue<Frame>();

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
            // if we have a valid frame and at least one user, put it in the queue
            if (frame != null && !frame.image.getMat().empty()) {
                var inFrameCopy = new Frame();
                frame.copyTo(inFrameCopy);
                frameQueue.add(inFrameCopy);
            }
        }
        oldSchoolServer.accept(frame);
    }

    /**
     * Empties the queue of frames to find the most recent then converts it to a jpeg and returns a
     * bytebuffer of the data
     *
     * @return
     */
    public ByteBuffer getJPEGByteBuffer() {

        Frame mostRecentFrame = null;

        // Empty the queue, releasing and discarding
        // any frame which is not the most recent
        while (true) {
            var tmp = frameQueue.poll();
            if (tmp == null) {
                break;
            } else {
                if (mostRecentFrame != null) {
                    mostRecentFrame.release();
                }
                mostRecentFrame = tmp;
            }
        }

        // If there was a non-null frame, convert it to a jpeg ByteBuffer
        ByteBuffer sendBuff = null;
        if (mostRecentFrame != null) {

            MatOfByte jpegBytes = new MatOfByte();
            Imgcodecs.imencode(
                    ".jpg",
                    mostRecentFrame.image.getMat(),
                    jpegBytes,
                    new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 75));

            sendBuff = ByteBuffer.wrap(jpegBytes.toArray());
            mostRecentFrame.release();
            jpegBytes.release();
        }

        return sendBuff;
    }

    public void addUser() {
        userCount++;
    }

    public void removeUser() {
        userCount--;
    }
}
