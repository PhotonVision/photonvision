/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.photonvision.jni;

import edu.wpi.first.util.RawFrame;
import edu.wpi.first.util.TimestampSource;

public class CscoreExtras {
    /**
     * Fill {@param framePtr} with the latest image from the source this sink is connected to.
     *
     * <p>If lastFrameTime is provided and non-zero, the sink will fill image with the first frame
     * from the source that is not equal to lastFrameTime. If lastFrameTime is zero, the time of the
     * current frame owned by the CvSource is used, and this function will block until the connected
     * CvSource provides a new frame.
     *
     * @param sink Sink handle.
     * @param framePtr Pointer to a wpi::RawFrame.
     * @param timeout Timeout in seconds.
     * @param lastFrameTime Timestamp of the last frame - used to compare new frames against.
     * @return Frame time, in uS, of the incoming frame.
     */
    public static native long grabRawSinkFrameTimeoutLastTime(
            int sink, long framePtr, double timeout, long lastFrameTime);

    /**
     * Wrap the data owned by a RawFrame in a cv::Mat
     *
     * @param rawFramePtr
     * @return pointer to a cv::Mat
     */
    public static native long wrapRawFrame(long rawFramePtr);

    private static native int getTimestampSourceNative(long rawFramePtr);

    public static TimestampSource getTimestampSource(RawFrame frame) {
        return TimestampSource.getFromInt(getTimestampSourceNative(frame.getNativeObj()));
    }
}
