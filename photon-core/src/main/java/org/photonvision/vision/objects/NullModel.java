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

package org.photonvision.vision.objects;

import java.util.List;
import org.opencv.core.Mat;
import org.photonvision.vision.pipe.impl.NeuralNetworkPipeResult;

/**
 * A 'null' implementation of the {@link Model} and {@link ObjectDetector} interfaces. This is used
 * when no model is available to load.
 */
public class NullModel implements Model, ObjectDetector {
    // Singleton instance
    public static final NullModel INSTANCE = new NullModel();

    private NullModel() {}

    public static NullModel getInstance() {
        return INSTANCE;
    }

    @Override
    public ObjectDetector load() {
        return this;
    }

    @Override
    public String getName() {
        return "NullModel";
    }

    @Override
    public void release() {
        // Do nothing
    }

    @Override
    public Model getModel() {
        return this;
    }

    @Override
    public List<String> getClasses() {
        return List.of();
    }

    @Override
    public List<NeuralNetworkPipeResult> detect(Mat in, double nmsThresh, double boxThresh) {
        return List.of();
    }
}
