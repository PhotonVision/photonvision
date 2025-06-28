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
import java.util.List;
import org.photonvision.common.dataflow.structures.PacketSerde;
import org.photonvision.struct.MultiTargetPNPResultSerde;
import org.photonvision.targeting.proto.MultiTargetPNPResultProto;
import org.photonvision.targeting.serde.PhotonStructSerializable;

public class MultiTargetPNPResult
        implements ProtobufSerializable, PhotonStructSerializable<MultiTargetPNPResult> {
    // Seeing 32 apriltags at once seems like a sane limit
    private static final int MAX_IDS = 32;

    public PnpResult estimatedPose = new PnpResult();
    public List<Short> fiducialIDsUsed = List.of();

    public MultiTargetPNPResult() {}

    public MultiTargetPNPResult(PnpResult results, List<Short> ids) {
        estimatedPose = results;
        fiducialIDsUsed = ids;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((estimatedPose == null) ? 0 : estimatedPose.hashCode());
        result = prime * result + ((fiducialIDsUsed == null) ? 0 : fiducialIDsUsed.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MultiTargetPNPResult other = (MultiTargetPNPResult) obj;
        if (estimatedPose == null) {
            if (other.estimatedPose != null) return false;
        } else if (!estimatedPose.equals(other.estimatedPose)) return false;
        if (fiducialIDsUsed == null) {
            if (other.fiducialIDsUsed != null) return false;
        } else if (!fiducialIDsUsed.equals(other.fiducialIDsUsed)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "MultiTargetPNPResult [estimatedPose="
                + estimatedPose
                + ", fiducialIDsUsed="
                + fiducialIDsUsed
                + "]";
    }

    public static final MultiTargetPNPResultProto proto = new MultiTargetPNPResultProto();

    // tODO!
    public static final MultiTargetPNPResultSerde photonStruct = new MultiTargetPNPResultSerde();

    @Override
    public PacketSerde<MultiTargetPNPResult> getSerde() {
        return photonStruct;
    }
}
