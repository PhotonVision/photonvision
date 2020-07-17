package org.photonvision.common.dataflow.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.wpi.first.wpilibj.MedianFilter;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.server.SocketHandler;
import org.photonvision.vision.pipeline.result.CVPipelineResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;

public class UIDataPublisher implements CVPipelineResultConsumer {
    private static final Logger logger = new Logger(UIDataPublisher.class, LogGroup.VisionModule);

    // TODO check if this is the right spot to do FPS calculation
    private final MedianFilter fpsAverager = new MedianFilter(10);
    private final int index;
    private long lastRunTime = 0;
    private long lastUIResultUpdateTime = 0;

    public UIDataPublisher(int index) {
        this.index = index;
    }

    @Override
    public void accept(CVPipelineResult result) {
        var now = System.currentTimeMillis();

        var fps = fpsAverager.calculate(1000.0 / (now - lastRunTime));
        lastRunTime = now;

        // only update the UI at 15hz
        if (lastUIResultUpdateTime + 1000.0 / 15.0 > now) return;

        var uiMap = new HashMap<Integer, HashMap<String, Object>>();
        var dataMap = new HashMap<String, Object>();

        dataMap.put("fps", fps);
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

        try {
            SocketHandler.getInstance().broadcastMessage(retMap, null);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
        }

        lastUIResultUpdateTime = now;
    }
}
