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
