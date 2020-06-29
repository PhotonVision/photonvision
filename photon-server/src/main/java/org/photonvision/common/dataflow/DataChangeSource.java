package org.photonvision.common.dataflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum DataChangeSource {
    DCS_WEBSOCKET,
    DCS_HTTP,
    DCS_NETWORKTABLES,
    DCS_VISIONMODULE,
    DCS_OTHER;

    public static final List<DataChangeSource> AllSources = Arrays.asList(DataChangeSource.values());

    public static class DataChangeSourceList extends ArrayList<DataChangeSource> {}
}
