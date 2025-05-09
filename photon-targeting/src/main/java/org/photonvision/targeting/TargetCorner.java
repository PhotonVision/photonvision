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

package org.photonvision.targeting;

import edu.wpi.first.util.protobuf.ProtobufSerializable;
import java.util.Objects;
import org.photonvision.common.dataflow.structures.PacketSerde;
import org.photonvision.struct.TargetCornerSerde;
import org.photonvision.targeting.proto.TargetCornerProto;
import org.photonvision.targeting.serde.PhotonStructSerializable;

/**
 * Represents a point in an image at the corner of the minimum-area bounding rectangle, in pixels.
 * Origin at the top left, plus-x to the right, plus-y down.
 */
public class TargetCorner implements ProtobufSerializable, PhotonStructSerializable<TargetCorner> {
    public double x;
    public double y;

    public TargetCorner(double cx, double cy) {
        this.x = cx;
        this.y = cy;
    }

    public TargetCorner() {
        this(0, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetCorner that = (TargetCorner) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ')';
    }

    public static final TargetCornerProto proto = new TargetCornerProto();
    public static final TargetCornerSerde photonStruct = new TargetCornerSerde();

    @Override
    public PacketSerde<TargetCorner> getSerde() {
        return photonStruct;
    }
}
