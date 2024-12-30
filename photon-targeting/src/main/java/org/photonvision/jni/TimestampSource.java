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

public enum TimestampSource {
    kUnknown(0),
    // wpi::Now when the new frame was dequeued by CSCore. Does not account for camera exposure time
    // or V4L latency.
    kFrameDequeue(1),
    // End of Frame. Same as V4L2_BUF_FLAG_TSTAMP_SRC_EOF, translated into wpi::Now's timebase.
    kV4lEoF(2),
    // Start of Exposure. Same as V4L2_BUF_FLAG_TSTAMP_SRC_SOE, translated into wpi::Now's timebase.
    kV4lSoE(3);

    private final int value;

    TimestampSource(int value) {
        this.value = value;
    }

    /**
     * Gets the integer value of the pixel format.
     *
     * @return Integer value
     */
    public int getValue() {
        return value;
    }

    private static final TimestampSource[] s_values = values();

    /**
     * Gets a TimestampSource enum value from its integer value.
     *
     * @param TimestampSource integer value
     * @return Enum value
     */
    public static TimestampSource getFromInt(int timestampSource) {
        return s_values[timestampSource];
    }
}
