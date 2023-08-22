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

package org.photonvision.common.dataflow.websocket;

import java.util.ArrayList;
import java.util.HashMap;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.SerializationUtils;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

public class UIDataPublisher implements CVPipelineResultConsumer {
    private static final Logger logger = new Logger(UIDataPublisher.class, LogGroup.VisionModule);

    private final int index;
    private long lastUIResultUpdateTime = 0;

    public UIDataPublisher(int index) {
        this.index = index;
    }

    @Override
    public void accept(CVPipelineResult result) {
        long now = System.currentTimeMillis();

        // only update the UI at 15hz
        if (lastUIResultUpdateTime + 1000.0 / 10.0 > now) return;

        var dataMap = new HashMap<String, Object>();
        dataMap.put("fps", result.fps);
        dataMap.put("latency", result.getLatencyMillis());
        var uiTargets = new ArrayList<HashMap<String, Object>>(result.targets.size());
        for (var t : result.targets) {
            uiTargets.add(t.toHashMap());
        }
        dataMap.put("targets", uiTargets);

        // Only send Multitag Results if they are present, similar to 3d pose
        if (result.multiTagResult.isPresent) {
            var multitagData = new HashMap<String, Object>();
            multitagData.put(
                    "bestTransform", SerializationUtils.transformToHashMap(result.multiTagResult.best));
            multitagData.put("bestReprojectionError", result.multiTagResult.bestReprojErr);
            dataMap.put("multitagResult", multitagData);
        }

        var uiMap = new HashMap<Integer, HashMap<String, Object>>();
        uiMap.put(index, dataMap);

        DataChangeService.getInstance()
                .publishEvent(OutgoingUIEvent.wrappedOf("updatePipelineResult", uiMap));
        lastUIResultUpdateTime = now;
    }
}
