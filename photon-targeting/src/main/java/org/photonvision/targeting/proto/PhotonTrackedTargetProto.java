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
