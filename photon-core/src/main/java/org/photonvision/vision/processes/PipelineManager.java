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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.pipeline.*;

@SuppressWarnings({"rawtypes", "unused"})
public class PipelineManager {
    private static final Logger logger = new Logger(PipelineManager.class, LogGroup.VisionModule);

    public static final int DRIVERMODE_INDEX = -1;
    public static final int CAL_3D_INDEX = -2;

    protected final List<CVPipelineSettings> userPipelineSettings;
    protected final Calibrate3dPipeline calibration3dPipeline = new Calibrate3dPipeline();
    protected final DriverModePipeline driverModePipeline = new DriverModePipeline();

    /** Index of the currently active pipeline. Defaults to 0. */
    private int currentPipelineIndex = 0;

    /** The currently active pipeline. */
    private CVPipeline currentUserPipeline = driverModePipeline;

    /**
    * Index of the last active user-created pipeline. <br>
    * <br>
    * Used only when switching from any of the built-in pipelines back to a user-created pipeline.
    */
    private int lastPipelineIndex;

    /**
    * Creates a PipelineManager with a DriverModePipeline, a Calibration3dPipeline, and all provided
    * pipelines.
    *
    * @param userPipelines Pipelines to add to the manager.
    */
    public PipelineManager(
            DriverModePipelineSettings driverSettings, List<CVPipelineSettings> userPipelines) {
        this.userPipelineSettings = new ArrayList<>(userPipelines);
        // This is to respect the default res idx for vendor cameras

        this.driverModePipeline.setSettings(driverSettings);

        if (userPipelines.size() < 1) addPipeline(PipelineType.Reflective);
    }

    public PipelineManager(CameraConfiguration config) {
        this(config.driveModeSettings, config.pipelineSettings);
    }

    /**
    * Get the settings for a pipeline by index.
    *
    * @param index Index of pipeline whose settings need getting.
    * @return The gotten settings of the pipeline whose index was provided.
    */
    public CVPipelineSettings getPipelineSettings(int index) {
        if (index < 0) {
            switch (index) {
                case DRIVERMODE_INDEX:
                    return driverModePipeline.getSettings();
                case CAL_3D_INDEX:
                    return calibration3dPipeline.getSettings();
            }
        }

        for (var setting : userPipelineSettings) {
            if (setting.pipelineIndex == index) return setting;
        }
        return null;
    }

    /**
    * Gets a list of nicknames for all user pipelines
    *
    * @return The list of nicknames for all user pipelines
    */
    public List<String> getPipelineNicknames() {
        List<String> ret = new ArrayList<>();
        for (var p : userPipelineSettings) {
            ret.add(p.pipelineNickname);
        }
        return ret;
    }

    /**
    * Gets the index of the currently active pipeline
    *
    * @return The index of the currently active pipeline
    */
    public int getCurrentPipelineIndex() {
        return currentPipelineIndex;
    }

    /**
    * Get the currently active pipeline.
    *
    * @return The currently active pipeline.
    */
    public CVPipeline getCurrentUserPipeline() {
        if (currentPipelineIndex < 0) {
            switch (currentPipelineIndex) {
                case CAL_3D_INDEX:
                    return calibration3dPipeline;
                case DRIVERMODE_INDEX:
                    return driverModePipeline;
            }
        }

        var desiredPipelineSettings = userPipelineSettings.get(currentPipelineIndex);
        //        if (currentPipeline.getSettings().pipelineIndex !=
        // desiredPipelineSettings.pipelineIndex) {
        //            switch (desiredPipelineSettings.pipelineType) {
        //                case Reflective:
        //                    currentPipeline =
        //                            new ReflectivePipeline((ReflectivePipelineSettings)
        // desiredPipelineSettings);
        //                    break;
        //                case ColoredShape:
        //                    currentPipeline =
        //                            new ColoredShapePipeline((ColoredShapePipelineSettings)
        // desiredPipelineSettings);
        //                    break;
        //            }
        //        }

        return currentUserPipeline;
    }

    /**
    * Get the currently active pipelines settings
    *
    * @return The currently active pipelines settings
    */
    public CVPipelineSettings getCurrentPipelineSettings() {
        return getPipelineSettings(currentPipelineIndex);
    }

    /**
    * Internal method for setting the active pipeline. <br>
    * <br>
    * All externally accessible methods that intend to change the active pipeline MUST go through
    * here to ensure all proper steps are taken.
    *
    * @param index Index of pipeline to be active
    */
    private void setPipelineInternal(int index) {
        if (index < 0) {
            lastPipelineIndex = currentPipelineIndex;
        }

        if (userPipelineSettings.size() - 1 < index) {
            logger.warn("User attempted to set index to non-existent pipeline!");
            return;
        }

        currentPipelineIndex = index;
        if (index >= 0) {
            var desiredPipelineSettings = userPipelineSettings.get(currentPipelineIndex);
            switch (desiredPipelineSettings.pipelineType) {
                case Reflective:
                    currentUserPipeline =
                            new ReflectivePipeline((ReflectivePipelineSettings) desiredPipelineSettings);
                    break;
                case ColoredShape:
                    currentUserPipeline =
                            new ColoredShapePipeline((ColoredShapePipelineSettings) desiredPipelineSettings);
                    break;
            }
        }
    }

    /**
    * Enters or exits calibration mode based on the parameter. <br>
    * <br>
    * Exiting returns to the last used user pipeline.
    *
    * @param wantsCalibration True to enter calibration mode, false to exit calibration mode.
    */
    public void setCalibrationMode(boolean wantsCalibration) {
        if (!wantsCalibration) calibration3dPipeline.finishCalibration();
        setPipelineInternal(wantsCalibration ? CAL_3D_INDEX : lastPipelineIndex);
    }

    /**
    * Enters or exits driver mode based on the parameter. <br>
    * <br>
    * Exiting returns to the last used user pipeline.
    *
    * @param state True to enter driver mode, false to exit driver mode.
    */
    public void setDriverMode(boolean state) {
        setPipelineInternal(state ? DRIVERMODE_INDEX : lastPipelineIndex);
    }

    /**
    * Returns whether or not driver mode is active.
    *
    * @return Whether or not driver mode is active.
    */
    public boolean getDriverMode() {
        return currentPipelineIndex == DRIVERMODE_INDEX;
    }

    public static final Comparator<CVPipelineSettings> PipelineSettingsIndexComparator =
            Comparator.comparingInt(o -> o.pipelineIndex);

    /**
    * Sorts the pipeline list by index, and reassigns their indexes to match the new order. <br>
    * <br>
    * I don't like this but I have no other ideas, and it works so ¯\_(ツ)_/¯
    */
    private void reassignIndexes() {
        userPipelineSettings.sort(PipelineSettingsIndexComparator);
        for (int i = 0; i < userPipelineSettings.size(); i++) {
            userPipelineSettings.get(i).pipelineIndex = i;
        }
    }

    public CVPipelineSettings addPipeline(PipelineType type) {
        return addPipeline(type, "New Pipeline");
    }

    public CVPipelineSettings addPipeline(PipelineType type, String nickname) {
        switch (type) {
            case Reflective:
                {
                    var added = new ReflectivePipelineSettings();
                    added.pipelineNickname = nickname;
                    addPipelineInternal(added);
                    return added;
                }
            case ColoredShape:
                {
                    var added = new ColoredShapePipelineSettings();
                    addPipelineInternal(added);
                    return added;
                }
        }
        reassignIndexes();
        return null;
    }

    private void addPipelineInternal(CVPipelineSettings settings) {
        settings.pipelineIndex = userPipelineSettings.size();
        userPipelineSettings.add(settings);
        reassignIndexes();
    }

    private void removePipelineInternal(int index) {
        userPipelineSettings.remove(index);
        currentPipelineIndex = Math.min(index, userPipelineSettings.size() - 1);
        reassignIndexes();
    }

    public void setIndex(int index) {
        this.setPipelineInternal(index);
    }

    public void removePipeline(int index) {
        if (index < 0) {
            return;
        }
        // TODO should we block/lock on a mutex?
        removePipelineInternal(index);
        setIndex(currentPipelineIndex);
    }

    public void renameCurrentPipeline(String newName) {
        getCurrentPipelineSettings().pipelineNickname = newName;
    }

    public void duplicatePipeline(int index) {
        var settings = userPipelineSettings.get(index);
        var newSettings = settings.clone();
        newSettings.pipelineNickname =
                createUniqueName(settings.pipelineNickname, userPipelineSettings);
        newSettings.pipelineIndex = Integer.MAX_VALUE;
        logger.debug("Duplicating pipe " + index + " to " + newSettings.pipelineNickname);
        userPipelineSettings.add(newSettings);
        reassignIndexes();
    }

    private static String createUniqueName(
            String nickname, List<CVPipelineSettings> existingSettings) {
        int index = 0;
        String uniqueName = nickname;
        while (true) {
            String finalUniqueName = uniqueName;
            var conflictingName =
                    existingSettings.stream().anyMatch(it -> it.pipelineNickname.equals(finalUniqueName));
            if (!conflictingName) return uniqueName;
            index++;
            uniqueName = nickname + " (" + index + ")";

            if (index == 6
                    && existingSettings.stream()
                            .noneMatch(it -> it.pipelineNickname.equals(nickname + "( dQw4w9WgXcQ )")))
                return nickname + "( dQw4w9WgXcQ )";
        }
    }
}
