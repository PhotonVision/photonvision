package org.photonvision.common.dataflow.networktables;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
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
