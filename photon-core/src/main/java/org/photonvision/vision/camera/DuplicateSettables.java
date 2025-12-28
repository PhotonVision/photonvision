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

package org.photonvision.vision.camera;

import edu.wpi.first.cscore.VideoMode;
import java.util.HashMap;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.processes.VisionSourceSettables;

/**
 * Read-only settables for duplicate cameras. All setter methods log warnings and do nothing, while
 * getter methods delegate to the source camera's settables.
 *
 * <p>This ensures that only the source (primary) camera can modify input settings like exposure,
 * gain, white balance, and resolution. Duplicate cameras share these settings from the source.
 */
public class DuplicateSettables extends VisionSourceSettables {
    private final VisionSourceSettables sourceSettables;

    public DuplicateSettables(CameraConfiguration config, VisionSourceSettables sourceSettables) {
        super(config);
        this.sourceSettables = sourceSettables;
    }

    // All setters log warnings and do nothing

    @Override
    public void setExposureRaw(double exposureRaw) {
        logger.warn(
                "Cannot change exposure on duplicate camera - this is controlled by the source camera");
    }

    @Override
    public void setAutoExposure(boolean cameraAutoExposure) {
        logger.warn(
                "Cannot change auto exposure on duplicate camera - this is controlled by the source camera");
    }

    @Override
    public void setBrightness(int brightness) {
        logger.warn(
                "Cannot change brightness on duplicate camera - this is controlled by the source camera");
    }

    @Override
    public void setGain(int gain) {
        logger.warn("Cannot change gain on duplicate camera - this is controlled by the source camera");
    }

    @Override
    public void setRedGain(int red) {
        logger.warn(
                "Cannot change red gain on duplicate camera - this is controlled by the source camera");
    }

    @Override
    public void setBlueGain(int blue) {
        logger.warn(
                "Cannot change blue gain on duplicate camera - this is controlled by the source camera");
    }

    @Override
    public void setWhiteBalanceTemp(double temp) {
        logger.warn(
                "Cannot change white balance temperature on duplicate camera - this is controlled by the source camera");
    }

    @Override
    public void setAutoWhiteBalance(boolean autowb) {
        logger.warn(
                "Cannot change auto white balance on duplicate camera - this is controlled by the source camera");
    }

    @Override
    protected void setVideoModeInternal(VideoMode videoMode) {
        logger.warn(
                "Cannot change video mode on duplicate camera - this is controlled by the source camera");
    }

    // All getters delegate to source

    @Override
    public VideoMode getCurrentVideoMode() {
        return sourceSettables.getCurrentVideoMode();
    }

    @Override
    public HashMap<Integer, VideoMode> getAllVideoModes() {
        return sourceSettables.getAllVideoModes();
    }

    @Override
    public double getMinExposureRaw() {
        return sourceSettables.getMinExposureRaw();
    }

    @Override
    public double getMaxExposureRaw() {
        return sourceSettables.getMaxExposureRaw();
    }

    @Override
    public double getMinWhiteBalanceTemp() {
        return sourceSettables.getMinWhiteBalanceTemp();
    }

    @Override
    public double getMaxWhiteBalanceTemp() {
        return sourceSettables.getMaxWhiteBalanceTemp();
    }

    /**
     * Always delegate to the source for frame static properties. This ensures duplicates always see
     * the current properties even if the source camera connects after the duplicate is created.
     */
    @Override
    public FrameStaticProperties getFrameStaticProperties() {
        return sourceSettables.getFrameStaticProperties();
    }

    /**
     * Override to do nothing since we always delegate to source. Duplicate cameras don't calculate
     * their own frame static properties.
     */
    @Override
    protected void calculateFrameStaticProps() {
        // No-op: We always delegate to source via getFrameStaticProperties()
    }
}
