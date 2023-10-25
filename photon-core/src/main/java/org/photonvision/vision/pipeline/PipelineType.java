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

package org.photonvision.vision.pipeline;

@SuppressWarnings("rawtypes")
public enum PipelineType {
    Calib3d(-2, Calibrate3dPipeline.class),
    DriverMode(-1, DriverModePipeline.class),
    Reflective(0, ReflectivePipeline.class),
    ColoredShape(1, ColoredShapePipeline.class),
    AprilTag(2, AprilTagPipeline.class),
    Aruco(3, ArucoPipeline.class);

    public final int baseIndex;
    public final Class clazz;

    PipelineType(int baseIndex, Class clazz) {
        this.baseIndex = baseIndex;
        this.clazz = clazz;
    }
}
