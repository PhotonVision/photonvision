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

package org.photonvision.vision.processes;

import io.javalin.websocket.WsContext;
import org.photonvision.vision.pipeline.CVPipelineSettings;

public class VisionModuleChange<T> {
    private final String propName;
    private final T newPropValue;
    private final CVPipelineSettings currentSettings;
    private final WsContext originContext;

    public VisionModuleChange(
            String propName,
            T newPropValue,
            CVPipelineSettings currentSettings,
            WsContext originContext) {
        this.propName = propName;
        this.newPropValue = newPropValue;
        this.currentSettings = currentSettings;
        this.originContext = originContext;
    }

    public String getPropName() {
        return propName;
    }

    public T getNewPropValue() {
        return newPropValue;
    }

    public CVPipelineSettings getCurrentSettings() {
        return currentSettings;
    }

    public WsContext getOriginContext() {
        return originContext;
    }
}
