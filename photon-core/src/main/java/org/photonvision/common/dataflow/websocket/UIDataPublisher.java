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
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
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
        var now = System.currentTimeMillis();

        // only update the UI at 15hz
        if (lastUIResultUpdateTime + 1000.0 / 10.0 > now) return;

        var uiMap = new HashMap<Integer, HashMap<String, Object>>();
        var dataMap = new HashMap<String, Object>();

        dataMap.put("fps", result.fps);
        dataMap.put("latency", result.getLatencyMillis());

        var targets = result.targets;

        var uiTargets = new ArrayList<HashMap<String, Object>>();
        for (var t : targets) {
            uiTargets.add(t.toHashMap());
        }
        dataMap.put("targets", uiTargets);

        uiMap.put(index, dataMap);
        var retMap = new HashMap<String, Object>();
        retMap.put("updatePipelineResult", uiMap);

        // TODO: MONOREPO - use pub/sub here

        //        try {
        //            SocketHandler.getInstance().broadcastMessage(retMap, null);
        //        } catch (JsonProcessingException e) {
        //            logger.error(e.getMessage());
        //            logger.error(Arrays.toString(e.getStackTrace()));
        //        }

        lastUIResultUpdateTime = now;
    }
}
