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

import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import java.util.HashMap;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.FrameStaticProperties;

public abstract class VisionSourceSettables {
    private static final Logger logger =
            new Logger(VisionSourceSettables.class, LogGroup.VisionModule);

    private final CameraConfiguration configuration;

    protected VisionSourceSettables(CameraConfiguration configuration) {
        this.configuration = configuration;
    }

    protected FrameStaticProperties frameStaticProperties;
    protected HashMap<Integer, VideoMode> videoModes;

    public CameraConfiguration getConfiguration() {
        return configuration;
    }

    public abstract void setExposure(double exposure);

    public abstract void setBrightness(int brightness);

    public abstract void setGain(int gain);

    public abstract VideoMode getCurrentVideoMode();

    public void setVideoModeInternal(int index) {
        setVideoMode(getAllVideoModes().get(index));
    }

    public void setVideoMode(VideoMode mode) {
        setVideoModeInternal(mode);
        calculateFrameStaticProps();
    }

    protected abstract void setVideoModeInternal(VideoMode videoMode);

    public void setCameraPitch(Rotation2d pitch) {
        configuration.camPitch = pitch;
        calculateFrameStaticProps();
    }

    public Rotation2d getCameraPitch() {
        return configuration.camPitch;
    }

    @SuppressWarnings("unused")
    public void setVideoModeIndex(int index) {
        setVideoMode(videoModes.get(index));
    }

    public abstract HashMap<Integer, VideoMode> getAllVideoModes();

    public double getFOV() {
        return configuration.FOV;
    }

    public void setFOV(double fov) {
        configuration.FOV = fov;
        calculateFrameStaticProps();
    }

    public void calculateFrameStaticProps() {
        var videoMode = getCurrentVideoMode();
        this.frameStaticProperties =
                new FrameStaticProperties(
                        videoMode,
                        getFOV(),
                        configuration.camPitch,
                        configuration.calibrations.stream()
                                .filter(
                                        it ->
                                                it.resolution.width == videoMode.width
                                                        && it.resolution.height == videoMode.height)
                                .findFirst()
                                .orElse(null));
    }

    public FrameStaticProperties getFrameStaticProperties() {
        return frameStaticProperties;
    }
}
