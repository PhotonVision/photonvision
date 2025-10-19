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

package org.photonvision.targeting.proto;

import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.util.protobuf.Protobuf;
import java.util.ArrayList;
import java.util.List;
import org.photonvision.proto.Photon.ProtobufPhotonTrackedTarget;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;
import us.hebi.quickbuf.Descriptors.Descriptor;
import us.hebi.quickbuf.RepeatedMessage;

public class PhotonTrackedTargetProto
        implements Protobuf<PhotonTrackedTarget, ProtobufPhotonTrackedTarget> {
    @Override
    public Class<PhotonTrackedTarget> getTypeClass() {
        return PhotonTrackedTarget.class;
    }

    @Override
    public Descriptor getDescriptor() {
        return ProtobufPhotonTrackedTarget.getDescriptor();
    }

    @Override
    public ProtobufPhotonTrackedTarget createMessage() {
        return ProtobufPhotonTrackedTarget.newInstance();
    }

    @Override
    public PhotonTrackedTarget unpack(ProtobufPhotonTrackedTarget msg) {
        return new PhotonTrackedTarget(
                msg.getYaw(),
                msg.getPitch(),
                msg.getArea(),
                msg.getSkew(),
                msg.getFiducialId(),
                msg.getObjDetectionId(),
                msg.getObjDetectionConf(),
                Transform3d.proto.unpack(msg.getBestCameraToTarget()),
                Transform3d.proto.unpack(msg.getAltCameraToTarget()),
                msg.getPoseAmbiguity(),
                TargetCorner.proto.unpack(msg.getMinAreaRectCorners()),
                TargetCorner.proto.unpack(msg.getDetectedCorners()));
    }

    public List<PhotonTrackedTarget> unpack(RepeatedMessage<ProtobufPhotonTrackedTarget> msg) {
        ArrayList<PhotonTrackedTarget> targets = new ArrayList<>(msg.length());
        for (ProtobufPhotonTrackedTarget target : msg) {
            targets.add(unpack(target));
        }
        return targets;
    }

    @Override
    public void pack(ProtobufPhotonTrackedTarget msg, PhotonTrackedTarget value) {
        msg.setYaw(value.getYaw())
                .setPitch(value.getPitch())
                .setSkew(value.getSkew())
                .setArea(value.getArea())
                .setFiducialId(value.getFiducialId())
                .setPoseAmbiguity(value.getPoseAmbiguity())
                .setObjDetectionConf(value.getDetectedObjectConfidence())
                .setObjDetectionId(value.getDetectedObjectClassID());

        Transform3d.proto.pack(msg.getMutableBestCameraToTarget(), value.getBestCameraToTarget());
        Transform3d.proto.pack(msg.getMutableAltCameraToTarget(), value.getAlternateCameraToTarget());

        TargetCorner.proto.pack(msg.getMutableMinAreaRectCorners(), value.getMinAreaRectCorners());
        TargetCorner.proto.pack(msg.getMutableDetectedCorners(), value.getDetectedCorners());
    }

    public void pack(
            RepeatedMessage<ProtobufPhotonTrackedTarget> msg, List<PhotonTrackedTarget> value) {
        var targets = msg.reserve(value.size());
        for (PhotonTrackedTarget trackedTarget : value) {
            var target = targets.next();
            pack(target, trackedTarget);
        }
    }
}
