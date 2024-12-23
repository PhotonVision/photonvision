package org.photonvision.common.util;

import org.photonvision.common.dataflow.networktables.NetworkTablesManager;

import edu.wpi.first.networktables.IntegerArrayPublisher;
import edu.wpi.first.networktables.IntegerPublisher;
import edu.wpi.first.networktables.StringArrayPublisher;

public class EpochPublisher {

    private IntegerArrayPublisher timePub;
    private StringArrayPublisher namePub;

    public EpochPublisher(String nickname) {
        this.timePub = NetworkTablesManager.getInstance().kRootTable.getSubTable(nickname).getIntegerArrayTopic("tracer/timesUs").publish();
        this.namePub = NetworkTablesManager.getInstance().kRootTable.getSubTable(nickname).getStringArrayTopic("tracer/epochNames").publish();
    }

    public void consume(OrderedTracer tracer) {
        timePub.set(tracer.getEpochTimes());
        namePub.set(tracer.getEpochNames());
    }
}
