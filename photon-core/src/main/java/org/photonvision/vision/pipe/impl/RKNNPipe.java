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

package org.photonvision.vision.pipe.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opencv.core.Rect2d;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipe.CVPipe;
import org.photonvision.vision.rknn.RKNNJNI;
import org.photonvision.vision.rknn.RKNNJNI.BoxRect;
import org.photonvision.vision.rknn.RKNNJNI.DetectionResultGroup;

public class RKNNPipe
        extends CVPipe<CVMat, List<NeuralNetworkPipeResult>, RKNNPipe.RKNNPipeParams> {
    private Map<String, RKNNJNI> models = new HashMap<>();

    public RKNNPipe() {}

    protected List<NeuralNetworkPipeResult> process(CVMat in) {
        var frame = in.getMat();

        // Make sure we don't get a weird empty frame
        if (frame.empty()) {
            return List.of();
        }

        var res =
                groupToNNList(
                        getModel(getParams().modelPath).detectAndDisplay(in.getMat().getNativeObjAddr()));
        var filtered = new ArrayList<NeuralNetworkPipeResult>();
        for (var det : res) {
            if (det.confidence >= getParams().confidenceThreshold) filtered.add(det);
        }
        return filtered;
    }

    private List<NeuralNetworkPipeResult> groupToNNList(DetectionResultGroup group) {
        if (group == null) return List.of();
        var list = new ArrayList<NeuralNetworkPipeResult>();
        for (var result : group.results)
            list.add(new NeuralNetworkPipeResult(boxToRect2d(result.box), (int) result.cls, result.conf));
        return list;
    }

    private Rect2d boxToRect2d(BoxRect box) {
        return new Rect2d(box.left, box.top, box.right - box.left, box.bottom - box.top);
    }

    private RKNNJNI getModel(String name) {
        if (!models.containsKey(name)) {
            addModel(name);
        }
        return models.get(name);
    }

    private void addModel(String name) {
        var rj = new RKNNJNI();
        rj.init(ConfigManager.getInstance().getRKNNModelsPath() + "/" + name + ".rknn");
        models.put(name, rj);
    }

    public static class RKNNPipeParams {
        public double confidenceThreshold;
        public String modelPath;

        public RKNNPipeParams() {}
    }
}
