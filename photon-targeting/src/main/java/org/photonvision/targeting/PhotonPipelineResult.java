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

package org.photonvision.targeting;

import edu.wpi.first.util.protobuf.Protobuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.photonvision.proto.Photon.ProtobufPhotonPipelineResult;
import us.hebi.quickbuf.Descriptors.Descriptor;

/** Represents a pipeline result from a PhotonCamera. */
public class PhotonPipelineResult {
    private static boolean HAS_WARNED = false;

    private final double latencyMillis;
    public final List<PhotonTrackedTarget> targets = new ArrayList<>();
    private final MultiTargetPNPResult multiTagResult;

    private double timestampSeconds = -1;

    /**
     * Constructs a pipeline result.
     *
     * @param latencyMillis The latency in the pipeline.
     * @param targets The list of targets identified by the pipeline.
     * @param result Result from multi-target PNP.
     */
    public PhotonPipelineResult(
            double latencyMillis, List<PhotonTrackedTarget> targets, MultiTargetPNPResult result) {
        this.latencyMillis = latencyMillis;
        this.targets.addAll(targets);
        this.multiTagResult = result;
    }

    /**
     * Constructs a pipeline result.
     *
     * @param latencyMillis The latency in the pipeline.
     * @param targets The list of targets identified by the pipeline.
     */
    public PhotonPipelineResult(double latencyMillis, List<PhotonTrackedTarget> targets) {
        this(latencyMillis, targets, null);
    }

    /**
     * Returns the best target in this pipeline result. If there are no targets, this method will
     * return null. The best target is determined by the target sort mode in the PhotonVision UI.
     *
     * @return The best target of the pipeline result.
     */
    public PhotonTrackedTarget getBestTarget() {
        if (!hasTargets() && !HAS_WARNED) {
            String errStr =
                    "This PhotonPipelineResult object has no targets associated with it! Please check hasTargets() "
                            + "before calling this method. For more information, please review the PhotonLib "
                            + "documentation at http://docs.photonvision.org";
            System.err.println(errStr);
            new Exception().printStackTrace();
            HAS_WARNED = true;
        }
        return hasTargets() ? targets.get(0) : null;
    }

    /**
     * Returns the latency in the pipeline.
     *
     * @return The latency in the pipeline.
     */
    public double getLatencyMillis() {
        return latencyMillis;
    }

    /**
     * Returns the estimated time the frame was taken, This is more accurate than using <code>
     * getLatencyMillis()</code>
     *
     * @return The timestamp in seconds, or -1 if this result has no timestamp set.
     */
    public double getTimestampSeconds() {
        return timestampSeconds;
    }

    /**
     * Sets the FPGA timestamp of this result in seconds.
     *
     * @param timestampSeconds The timestamp in seconds.
     */
    public void setTimestampSeconds(double timestampSeconds) {
        this.timestampSeconds = timestampSeconds;
    }

    /**
     * Returns whether the pipeline has targets.
     *
     * @return Whether the pipeline has targets.
     */
    public boolean hasTargets() {
        return !targets.isEmpty();
    }

    /**
     * Returns a copy of the vector of targets.
     *
     * @return A copy of the vector of targets.
     */
    public List<PhotonTrackedTarget> getTargets() {
        return new ArrayList<>(targets);
    }

    /**
     * Return the MultiTarget Result. Empty if disabled or unable to create result.
     *
     * @return multitarget result
     */
    public Optional<MultiTargetPNPResult> getMultiTagResult() {
        return Optional.ofNullable(multiTagResult);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + targets.hashCode();
        long temp;
        temp = Double.doubleToLongBits(latencyMillis);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(timestampSeconds);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((multiTagResult == null) ? 0 : multiTagResult.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PhotonPipelineResult other = (PhotonPipelineResult) obj;
        if (!targets.equals(other.targets)) return false;
        if (Double.doubleToLongBits(latencyMillis) != Double.doubleToLongBits(other.latencyMillis))
            return false;
        if (Double.doubleToLongBits(timestampSeconds)
                != Double.doubleToLongBits(other.timestampSeconds)) return false;
        if (multiTagResult == null) {
            if (other.multiTagResult != null) return false;
        } else if (!multiTagResult.equals(other.multiTagResult)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "PhotonPipelineResult [targets="
                + targets
                + ", latencyMillis="
                + latencyMillis
                + ", timestampSeconds="
                + timestampSeconds
                + ", multiTagResult="
                + multiTagResult
                + "]";
    }

    public static final class AProto
            implements Protobuf<PhotonPipelineResult, ProtobufPhotonPipelineResult> {
        @Override
        public Class<PhotonPipelineResult> getTypeClass() {
            return PhotonPipelineResult.class;
        }

        @Override
        public Descriptor getDescriptor() {
            return ProtobufPhotonPipelineResult.getDescriptor();
        }

        @Override
        public Protobuf<?, ?>[] getNested() {
            return new Protobuf<?, ?>[] {PhotonTrackedTarget.proto, MultiTargetPNPResult.proto};
        }

        @Override
        public ProtobufPhotonPipelineResult createMessage() {
            return ProtobufPhotonPipelineResult.newInstance();
        }

        @Override
        public PhotonPipelineResult unpack(ProtobufPhotonPipelineResult msg) {
            return new PhotonPipelineResult(
                    msg.getLatencyMs(),
                    PhotonTrackedTarget.proto.unpack(msg.getTargets()),
                    msg.tryGetMultiTargetResult().map(MultiTargetPNPResult.proto::unpack).orElse(null));
        }

        @Override
        public void pack(ProtobufPhotonPipelineResult msg, PhotonPipelineResult value) {
            PhotonTrackedTarget.proto.pack(msg.getMutableTargets(), value.targets);
            if (value.multiTagResult != null) {
                MultiTargetPNPResult.proto.pack(msg.getMutableMultiTargetResult(), value.multiTagResult);
            }
            msg.setLatencyMs(value.getLatencyMillis());
        }
    }

    public static final AProto proto = new AProto();
}
