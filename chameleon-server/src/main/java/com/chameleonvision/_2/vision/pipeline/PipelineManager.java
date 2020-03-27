package com.chameleonvision._2.vision.pipeline;

import com.chameleonvision._2.config.CameraConfig;
import com.chameleonvision._2.config.ConfigManager;
import com.chameleonvision._2.vision.VisionManager;
import com.chameleonvision._2.vision.VisionProcess;
import com.chameleonvision._2.vision.pipeline.impl.Calibrate3dPipeline;
import com.chameleonvision._2.vision.pipeline.impl.DriverVisionPipeline;
import com.chameleonvision._2.vision.pipeline.impl.StandardCVPipeline;
import com.chameleonvision._2.vision.pipeline.impl.StandardCVPipelineSettings;
import com.chameleonvision._2.web.SocketHandler;
import com.chameleonvision.common.scripting.ScriptEventType;
import com.chameleonvision.common.scripting.ScriptManager;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.networktables.NetworkTableEntry;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class PipelineManager {

    private static final int DRIVERMODE_INDEX = -1;
    private static final int CAL_3D_INDEX = -2;

    public final LinkedList<CVPipeline> pipelines = new LinkedList<>();

    public final CVPipeline driverModePipeline = new DriverVisionPipeline(new CVPipelineSettings());
    public final Calibrate3dPipeline calib3dPipe = new Calibrate3dPipeline(new StandardCVPipelineSettings());

    private final VisionProcess parentProcess;
    private int lastPipelineIndex;
    private int currentPipelineIndex;
    public NetworkTableEntry ntIndexEntry;

    public PipelineManager(VisionProcess visionProcess, List<CVPipelineSettings> loadedPipelineSettings) {
        parentProcess = visionProcess;
        if (loadedPipelineSettings == null || loadedPipelineSettings.size() == 0) {
            pipelines.add(new StandardCVPipeline("New Pipeline"));
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
        if (setting instanceof StandardCVPipelineSettings) {
            pipelines.add(new StandardCVPipeline((StandardCVPipelineSettings) setting));
        } else {
            System.out.println("Non 2D/3D pipelines not supported!");
        }
        reassignIndexes();
    }

    public void setDriverMode(boolean driverMode) {
        if (driverMode) setCurrentPipeline(DRIVERMODE_INDEX);
        else setCurrentPipeline(lastPipelineIndex);
    }

    public void setCalibrationMode(boolean calibrationMode) {
        setCurrentPipeline((calibrationMode ? CAL_3D_INDEX : lastPipelineIndex));
    }

    public void enableCalibrationMode(VideoMode mode) {
        parentProcess.setVideoMode(mode);
        calib3dPipe.setVideoMode(mode);
        setCalibrationMode(true);
    }

    public boolean getDriverMode() {
        return currentPipelineIndex == DRIVERMODE_INDEX;
    }

    public int getCurrentPipelineIndex() {
        return currentPipelineIndex;
    }

    public CVPipeline getCurrentPipeline() {
        if (currentPipelineIndex == DRIVERMODE_INDEX) {
            return driverModePipeline;
        } else if (currentPipelineIndex <= CAL_3D_INDEX) {
          return calib3dPipe;
        } else {
            return pipelines.get(currentPipelineIndex);
        }
    }

    public void setCurrentPipeline(int index) {
        CVPipeline newPipeline = null;

        if (index == DRIVERMODE_INDEX) {
            ScriptManager.queueEvent(ScriptEventType.kLEDOff);
            newPipeline = driverModePipeline;

            // if we're changing into driver mode, try to set the nt entry to true
            parentProcess.setDriverModeEntry(true);
        } else if (index == CAL_3D_INDEX) {
            parentProcess.setDriverModeEntry(true);

            newPipeline = calib3dPipe;
        } else {
            if (index < pipelines.size()&&index>=0) {
                newPipeline = pipelines.get(index);

                // if we're switching out of driver mode, try to set the nt entry to false
                parentProcess.setDriverModeEntry(false);
                ScriptManager.queueEvent(ScriptEventType.kLEDOn);
            }
            else
                {
                    //TODO alert/warn user that pipeline doesnt exsits
                    System.err.println("Index is out of bounds");
                }
        }
        if (newPipeline != null) {
            lastPipelineIndex = currentPipelineIndex;
            currentPipelineIndex = index;
            getCurrentPipeline().initPipeline(parentProcess.getCamera());

            if (ConfigManager.settings.currentCamera.equals(parentProcess.getCamera().getProperties().name)) {
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
            if (parentProcess.cameraStreamer != null)
                parentProcess.cameraStreamer.setDivisor(newPipeline.settings.streamDivisor, true);
            if (ntIndexEntry != null) {
                ntIndexEntry.setDouble(index);
            }
        }

        // gain setting quirk
        if (!parentProcess.cameraCapture.getProperties().isPS3Eye) {
            getCurrentPipeline().settings.gain = -1;
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

    public void addNewPipeline(String piplineName) {
        StandardCVPipeline newPipeline = new StandardCVPipeline();
        newPipeline.settings.nickname = piplineName;
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
        if (destinationProcess.pipelineManager.pipelines.stream().anyMatch(c -> c.settings.nickname.equals(pipeline.nickname))){
//         throw new DuplicatedKeyException("key Already exists");
        } else{
            destinationProcess.pipelineManager.addPipeline(pipeline);
        }
    }

    public void renameCurrentPipeline(String newName) {
        CVPipelineSettings settings = getCurrentPipeline().settings;
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
