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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.opencv.aruco.Aruco;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
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
    private int lastUserPipelineIdx;

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
     * Get the settings for a pipeline by index.
     *
     * @param index Index of pipeline whose nickname needs getting.
     * @return the nickname of the pipeline whose index was provided.
     */
    public String getPipelineNickname(int index) {
        if (index < 0) {
            switch (index) {
                case DRIVERMODE_INDEX:
                    return driverModePipeline.getSettings().pipelineNickname;
                case CAL_3D_INDEX:
                    return calibration3dPipeline.getSettings().pipelineNickname;
            }
        }

        for (var setting : userPipelineSettings) {
            if (setting.pipelineIndex == index) return setting.pipelineNickname;
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
    public CVPipeline getCurrentPipeline() {
        if (currentPipelineIndex < 0) {
            switch (currentPipelineIndex) {
                case CAL_3D_INDEX:
                    return calibration3dPipeline;
                case DRIVERMODE_INDEX:
                    return driverModePipeline;
            }
        }

        // Just return the current user pipeline, we're not on aa built-in one
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
     * @param newIndex Index of pipeline to be active
     */
    private void setPipelineInternal(int newIndex) {
        if (newIndex < 0 && currentPipelineIndex >= 0) {
            // Transitioning to a built-in pipe, save off the current user one
            lastUserPipelineIdx = currentPipelineIndex;
        }

        if (userPipelineSettings.size() - 1 < newIndex) {
            logger.warn("User attempted to set index to non-existent pipeline!");
            return;
        }

        currentPipelineIndex = newIndex;
        if (newIndex >= 0) {
            var desiredPipelineSettings = userPipelineSettings.get(currentPipelineIndex);
            switch (desiredPipelineSettings.pipelineType) {
                case Reflective:
                    logger.debug("Creating Reflective pipeline");
                    currentUserPipeline =
                            new ReflectivePipeline((ReflectivePipelineSettings) desiredPipelineSettings);
                    break;
                case ColoredShape:
                    logger.debug("Creating ColoredShape pipeline");
                    currentUserPipeline =
                            new ColoredShapePipeline((ColoredShapePipelineSettings) desiredPipelineSettings);
                    break;
                case AprilTag:
                    logger.debug("Creating AprilTag pipeline");
                    currentUserPipeline =
                            new AprilTagPipeline((AprilTagPipelineSettings) desiredPipelineSettings);
                    break;

                case Aruco:
                    logger.debug("Creating Aruco Pipeline");
                    currentUserPipeline = new ArucoPipeline((ArucoPipelineSettings) desiredPipelineSettings);
                    break;
                default:
                    // Can be calib3d or drivermode, both of which are special cases
                    break;
            }
        }

        DataChangeService.getInstance()
                .publishEvent(
                        new OutgoingUIEvent<>(
                                "fullsettings", ConfigManager.getInstance().getConfig().toHashMap()));
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
        setPipelineInternal(wantsCalibration ? CAL_3D_INDEX : lastUserPipelineIdx);
    }

    /**
     * Enters or exits driver mode based on the parameter. <br>
     * <br>
     * Exiting returns to the last used user pipeline.
     *
     * @param state True to enter driver mode, false to exit driver mode.
     */
    public void setDriverMode(boolean state) {
        setPipelineInternal(state ? DRIVERMODE_INDEX : lastUserPipelineIdx);
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
        var added = createSettingsForType(type, nickname);
        if (added == null) {
            logger.error("Cannot add null pipeline!");
            return null;
        }
        addPipelineInternal(added);
        reassignIndexes();
        return added;
    }

    private CVPipelineSettings createSettingsForType(PipelineType type, String nickname) {
        CVPipelineSettings newSettings;
        switch (type) {
            case Reflective:
                {
                    var added = new ReflectivePipelineSettings();
                    added.pipelineNickname = nickname;
                    return added;
                }
            case ColoredShape:
                {
                    var added = new ColoredShapePipelineSettings();
                    added.pipelineNickname = nickname;
                    return added;
                }
            case AprilTag:
                {
                    var added = new AprilTagPipelineSettings();
                    added.pipelineNickname = nickname;
                    return added;
                }
            case Aruco:
                {
                    var added = new ArucoPipelineSettings();
                    added.pipelineNickname = nickname;
                    return added;
                }
            default:
                {
                    logger.error("Got invalid pipeline type: " + type.toString());
                    return null;
                }
        }
    }

    private void addPipelineInternal(CVPipelineSettings settings) {
        settings.pipelineIndex = userPipelineSettings.size();
        userPipelineSettings.add(settings);
        reassignIndexes();
    }

    /**
     * Remove a pipeline settings at the given index and return the new current index
     *
     * @param index The idx to remove
     */
    private int removePipelineInternal(int index) {
        userPipelineSettings.remove(index);
        currentPipelineIndex = Math.min(index, userPipelineSettings.size() - 1);
        reassignIndexes();
        return currentPipelineIndex;
    }

    public void setIndex(int index) {
        this.setPipelineInternal(index);
    }

    public int removePipeline(int index) {
        if (index < 0) {
            return currentPipelineIndex;
        }
        // TODO should we block/lock on a mutex?
        return removePipelineInternal(index);
    }

    public void renameCurrentPipeline(String newName) {
        getCurrentPipelineSettings().pipelineNickname = newName;
    }

    /**
     * Duplicate a pipeline at a given index
     *
     * @param index the index of the target pipeline
     * @return The new index
     */
    public int duplicatePipeline(int index) {
        var settings = userPipelineSettings.get(index);
        var newSettings = settings.clone();
        newSettings.pipelineNickname =
                createUniqueName(settings.pipelineNickname, userPipelineSettings);
        newSettings.pipelineIndex = Integer.MAX_VALUE;
        logger.debug("Duplicating pipe " + index + " to " + newSettings.pipelineNickname);
        userPipelineSettings.add(newSettings);
        reassignIndexes();

        // Now we look for the index of the new pipeline and return it
        return userPipelineSettings.indexOf(newSettings);
    }

    private static String createUniqueName(
            String nickname, List<CVPipelineSettings> existingSettings) {
        String uniqueName = nickname;
        while (true) {
            String finalUniqueName = uniqueName; // To get around lambda capture
            var conflictingName =
                    existingSettings.stream().anyMatch(it -> it.pipelineNickname.equals(finalUniqueName));

            if (!conflictingName) {
                // If no conflict, we're done
                return uniqueName;
            } else {
                // Otherwise, we need to add a suffix to the name
                // If the string doesn't already end in "([0-9]*)", we'll add it
                // If it does, we'll increment the number in the suffix

                if (uniqueName.matches(".*\\([0-9]*\\)")) {
                    // Because java strings are immutable, we have to do this curstedness
                    // This is like doing "New pipeline (" + 2 + ")"

                    var parenStart = uniqueName.lastIndexOf('(');
                    var parenEnd = uniqueName.length() - 1;
                    var number = Integer.parseInt(uniqueName.substring(parenStart + 1, parenEnd)) + 1;

                    uniqueName = uniqueName.substring(0, parenStart + 1) + number + ")";
                } else {
                    uniqueName += " (1)";
                }
            }
        }
    }

    public void changePipelineType(int newType) {
        // Find the PipelineType proposed
        // To do this we look at all the PipelineType entries and look for one with matching
        // base indexes
        PipelineType type =
                Arrays.stream(PipelineType.values())
                        .filter(it -> it.baseIndex == newType)
                        .findAny()
                        .orElse(null);
        if (type == null) {
            logger.error("Could not match type " + newType + " to a PipelineType!");
            return;
        }

        if (type.baseIndex == getCurrentPipelineSettings().pipelineType.baseIndex) {
            logger.debug(
                    "Not changing settings as "
                            + type
                            + " and "
                            + getCurrentPipelineSettings().pipelineType
                            + " are identical!");
            return;
        }

        // Our new settings will be totally nuked, but that's ok
        // We *could* set things in common between the two, if we want
        // But they're different enough it shouldn't be an issue
        var name = getCurrentPipelineSettings().pipelineNickname;
        var newSettings = createSettingsForType(type, name);

        var idx = currentPipelineIndex;
        if (idx < 0) {
            logger.error("Cannot replace non-user pipeline!");
            return;
        }

        logger.info("Adding new pipe of type " + type.toString() + " at idx " + idx);
        newSettings.pipelineIndex = idx;
        userPipelineSettings.set(idx, newSettings);
        setPipelineInternal(idx);
        reassignIndexes();
    }
}
