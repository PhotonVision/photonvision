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
