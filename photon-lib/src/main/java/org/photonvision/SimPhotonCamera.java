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

package org.photonvision;

import edu.wpi.first.networktables.NetworkTable;
import java.util.Arrays;
import java.util.List;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

public class SimPhotonCamera extends PhotonCamera {
    /**
    * Constructs a Simulated PhotonCamera from a root table.
    *
    * @param rootTable The root table that the camera is broadcasting information over.
    */
    public SimPhotonCamera(NetworkTable rootTable) {
        super(rootTable);
    }

    /**
    * Constructs a Simulated PhotonCamera from the name of the camera.
    *
    * @param cameraName The nickname of the camera (found in the PhotonVision UI).
    */
    public SimPhotonCamera(String cameraName) {
        super(cameraName);
    }

    /**
    * Simulate one processed frame of vision data, putting one result to NT.
    *
    * @param latencyMillis
    * @param targets Each target detected
    */
    public void submitProcessedFrame(double latencyMillis, PhotonTrackedTarget... targets) {
        submitProcessedFrame(latencyMillis, Arrays.asList(targets));
    }

    /**
    * Simulate one processed frame of vision data, putting one result to NT.
    *
    * @param latencyMillis
    * @param tgtList List of targets detected
    */
    public void submitProcessedFrame(double latencyMillis, List<PhotonTrackedTarget> tgtList) {
        if (!getDriverMode()) {
            PhotonPipelineResult newResult = new PhotonPipelineResult(latencyMillis, tgtList);
            var newPacket = new Packet(newResult.getPacketSize());
            newResult.populatePacket(newPacket);
            rawBytesEntry.setRaw(newPacket.getData());
        }
    }
}
