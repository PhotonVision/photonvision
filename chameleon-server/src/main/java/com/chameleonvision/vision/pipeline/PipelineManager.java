package com.chameleonvision.vision.pipeline;

import com.chameleonvision.config.CameraConfig;
import com.chameleonvision.config.ConfigManager;
import com.chameleonvision.vision.VisionManager;
import com.chameleonvision.vision.VisionProcess;
import com.chameleonvision.vision.pipeline.impl.*;
import com.chameleonvision.web.SocketHandler;
import edu.wpi.first.networktables.NetworkTableEntry;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class PipelineManager {

    private static final int DRIVERMODE_INDEX = -1;

    public final LinkedList<CVPipeline> pipelines = new LinkedList<>();

    public final CVPipeline driverModePipeline = new DriverVisionPipeline(new CVPipelineSettings());

    private final VisionProcess parentProcess;
    private int lastPipelineIndex;
    private int currentPipelineIndex;
    public NetworkTableEntry ntIndexEntry;

    public PipelineManager(VisionProcess visionProcess, List<CVPipelineSettings> loadedPipelineSettings) {
        parentProcess = visionProcess;
        if (loadedPipelineSettings == null || loadedPipelineSettings.size() == 0) {
            pipelines.add(new CVPipeline2d("New Pipeline"));
        } else {
            for (CVPipelineSettings setting : loadedPipelineSettings) {
                addInternalPipeline(setting);
            }
        }
        driverModePipeline.initPipeline(visionProcess.getCamera());
        setCurrentPipeline(0);
    }

    private void reassignIndexes() {
        pipelines.sort(IndexComparator);
        for (int i = 0; i < pipelines.size(); i++) {
            pipelines.get(i).settings.index = i;
        }
    }

    private CameraConfig getConfig(VisionProcess process) {
        return VisionManager.getCameraConfig(process);
    }

    private CameraConfig getConfig() {
        return getConfig(parentProcess);
    }

    private void savePipelineConfig(CVPipelineSettings setting) {
        getConfig().pipelineConfig.save(setting);
    }

    private void deletePipelineConfig(CVPipelineSettings setting) {
        getConfig().pipelineConfig.delete(setting);
    }

    private void renamePipelineConfig(CVPipelineSettings setting, String newName) {
        getConfig().pipelineConfig.rename(setting, newName);
    }

    public void saveAllPipelines() {
        pipelines.parallelStream().map(pipeline -> pipeline.settings).forEach(this::savePipelineConfig);
    }

    private void addInternalPipeline(CVPipelineSettings setting) {
        if (setting instanceof CVPipeline3dSettings) {
            pipelines.add(new CVPipeline3d((CVPipeline3dSettings) setting));
        } else if (setting instanceof CVPipeline2dSettings) {
            pipelines.add(new CVPipeline2d((CVPipeline2dSettings) setting));
        } else {
            System.out.println("Non 2D/3D pipelines not supported!");
        }
        reassignIndexes();
    }

    public void setDriverMode(boolean driverMode) {
        if (driverMode) {
            setCurrentPipeline(DRIVERMODE_INDEX);
        } else {
            setCurrentPipeline(lastPipelineIndex);
        }
    }

    public boolean getDriverMode() {
        return currentPipelineIndex == DRIVERMODE_INDEX;
    }

    public int getCurrentPipelineIndex() {
        return currentPipelineIndex;
    }

    public CVPipeline getCurrentPipeline() {
        if (currentPipelineIndex <= DRIVERMODE_INDEX) {
            return driverModePipeline;
        } else {
            return pipelines.get(currentPipelineIndex);
        }
    }

    public void setCurrentPipeline(int index) {
        CVPipeline newPipeline;
        if (index == DRIVERMODE_INDEX) {
            newPipeline = driverModePipeline;

            // if we're changing into driver mode, try to set the nt entry to true
            parentProcess.setDriverModeEntry(true);
        } else {
            newPipeline = pipelines.get(index);

            // if we're switching out of driver mode, try to set the nt entry to false
            parentProcess.setDriverModeEntry(false);
        }
        if (newPipeline != null) {
            lastPipelineIndex = currentPipelineIndex;
            currentPipelineIndex = index;
            getCurrentPipeline().initPipeline(parentProcess.getCamera());

            if(ConfigManager.settings.currentCamera.equals(parentProcess.getCamera().getProperties().name)) {
                ConfigManager.settings.currentPipeline = currentPipelineIndex;

                HashMap<String, Object> pipeChange = new HashMap<>();
                pipeChange.put("currentPipeline", currentPipelineIndex);
                SocketHandler.broadcastMessage(pipeChange);
                try {
                    SocketHandler.sendFullSettings();
                } catch (Exception e) {
                    // avoid NullPointerException when run before threads start
                }
            }
            newPipeline.initPipeline(parentProcess.getCamera());
            if(ntIndexEntry != null) {
                ntIndexEntry.setDouble(index);
            }
        }
    }

    public void addPipeline(CVPipelineSettings setting) {
        addInternalPipeline(setting);
        savePipelineConfig(setting);
    }

    public void addPipeline(CVPipeline pipeline) {
        pipelines.add(pipeline);
        reassignIndexes();
        savePipelineConfig(pipeline.settings);
    }

    public void addNewPipeline(boolean is3D) {
        CVPipeline newPipeline;
        if (!is3D) {
            newPipeline = new CVPipeline2d();
        } else {
            newPipeline = new CVPipeline3d();
        }
        newPipeline.settings.index = pipelines.size();
        addPipeline(newPipeline);
    }

    public CVPipeline getPipeline(int index) {
        return pipelines.get(index);
    }

    public void duplicatePipeline(CVPipelineSettings pipeline) {
        duplicatePipeline(pipeline, parentProcess);
    }

    public void duplicatePipeline(CVPipelineSettings pipeline, VisionProcess destinationProcess) {
        pipeline.index = destinationProcess.pipelineManager.pipelines.size();
        pipeline.nickname += "(Copy)";
        destinationProcess.pipelineManager.addPipeline(pipeline);
    }

    public void renameCurrentPipeline(String newName) {
        CVPipelineSettings settings = getCurrentPipeline().settings;
        settings.nickname = newName;
        renamePipelineConfig(settings, newName);
    }

    public void deleteCurrentPipeline() {
        deletePipeline(currentPipelineIndex);
    }

    private void deletePipeline(int index) {
        if (index == currentPipelineIndex) {
            currentPipelineIndex -= 1;
        }
        deletePipelineConfig(getPipeline(index).settings);
        pipelines.remove(index);
        reassignIndexes();
    }

    public void saveDriverModeConfig() {
        getConfig().saveDriverMode(driverModePipeline.settings);
    }

    private static final Comparator<CVPipeline> IndexComparator = (o1, o2) -> {
        int o1Index = o1.settings.index;
        int o2Index = o2.settings.index;

        if (o1Index == o2Index) {
            return 0;
        } else if (o1Index < o2Index) {
            return -1;
        }
        return 1;
    };
}
