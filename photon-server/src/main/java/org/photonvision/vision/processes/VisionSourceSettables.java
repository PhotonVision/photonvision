/*
 * Copyright (C) 2020 Photon Vision.
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

import edu.wpi.cscore.VideoMode;
import java.util.HashMap;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.frame.FrameStaticProperties;

public abstract class VisionSourceSettables {
    private CameraConfiguration configuration;

    protected VisionSourceSettables(CameraConfiguration configuration) {
        this.configuration = configuration;
    }

    FrameStaticProperties frameStaticProperties;
    protected HashMap<Integer, VideoMode> videoModes;

    public abstract int getExposure();

    public abstract void setExposure(int exposure);

    public abstract int getBrightness();

    public abstract void setBrightness(int brightness);

    public abstract int getGain();

    public abstract void setGain(int gain);

    public abstract VideoMode getCurrentVideoMode();

    public abstract void setCurrentVideoMode(VideoMode videoMode);

    public abstract HashMap<Integer, VideoMode> getAllVideoModes();

    public double getFOV() {
        return configuration.FOV;
    }

    public void setFOV(double fov) {
        configuration.FOV = fov;
    }

    public FrameStaticProperties getFrameStaticProperties() {
        return frameStaticProperties;
    }
}
