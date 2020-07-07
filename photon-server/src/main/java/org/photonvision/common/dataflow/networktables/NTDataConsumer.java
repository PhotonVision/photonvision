/*
 * Copyright (C) 2020 Photon Vision.
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

package org.photonvision.common.dataflow.networktables;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import org.photonvision.common.datatransfer.DataConsumer;
import org.photonvision.vision.pipeline.result.SimplePipelineResult;
import org.photonvision.vision.processes.Data;

public class NTDataConsumer implements DataConsumer {

    private final NetworkTable rootTable;
    NetworkTable subTable;
    NetworkTableEntry rawData;

    public NTDataConsumer(NetworkTable root, String camName) {
        this.rootTable = root;
        this.subTable = root.getSubTable(camName);
        rawData = subTable.getEntry("rawData");
    }

    public void setCameraName(String camName) {
        this.subTable = rootTable.getSubTable(camName);
        rawData = subTable.getEntry("rawData");
    }

    @Override
    public void accept(Data data) {
        var simplified = new SimplePipelineResult(data.result);
        var bytes = simplified.toByteArray();
        rawData.setRaw(bytes);
        rootTable.getInstance().flush();
    }
}
