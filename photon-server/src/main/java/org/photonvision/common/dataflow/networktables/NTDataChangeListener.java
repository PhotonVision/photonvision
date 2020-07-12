package org.photonvision.common.dataflow.networktables;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.EntryNotification;
import edu.wpi.first.networktables.NetworkTableEntry;

import java.util.function.Consumer;

public class NTDataChangeListener {

    private final NetworkTableEntry watchedEntry;
    private final int listenerID;

    public NTDataChangeListener(NetworkTableEntry watchedEntry, Consumer<EntryNotification> dataChangeConsumer) {
        this.watchedEntry = watchedEntry;
        listenerID = watchedEntry.addListener(dataChangeConsumer, EntryListenerFlags.kUpdate);
    }

    public void remove() {
        watchedEntry.removeListener(listenerID);
    }
}
