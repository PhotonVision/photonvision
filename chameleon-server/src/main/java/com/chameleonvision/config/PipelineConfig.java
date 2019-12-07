package com.chameleonvision.config;

import com.chameleonvision.util.JacksonHelper;
import com.chameleonvision.vision.pipeline.*;
import com.chameleonvision.vision.pipeline.impl.CVPipeline2dSettings;
import com.chameleonvision.vision.pipeline.impl.CVPipeline3dSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PipelineConfig {

    private static final String CVPipeline2DPrefix = "CV2D";
    private static final String CVPipeline3DPrefix = "CV3D";

    private final CameraConfig cameraConfig;

    /**
     * Construct a new PipelineConfig
     * @param cameraConfig the CameraConfig (parent folder, kinda?)
     */
    PipelineConfig(CameraConfig cameraConfig) {
        this.cameraConfig = cameraConfig;
    }

    private void checkFolder() {
        if ( !(new File(cameraConfig.getPipelineFolderPath().toUri()).mkdirs())) {
            if (Files.notExists(cameraConfig.getPipelineFolderPath())) {
                System.err.println("Failed to create pipelines folder.");
            }
        }
    }

    private File[] getPipelineFiles() {
        return new File(cameraConfig.getPipelineFolderPath().toUri()).listFiles();
    }

    private boolean folderHasPipelines() {
        File[] folderContents = getPipelineFiles();
        if(folderContents == null) return false;
        return folderContents.length > 0;
    }

    void check() {
        cameraConfig.checkFolder();
        checkFolder();
        // Check if there's at least one pipe
        if (!folderHasPipelines()) {
            save(new CVPipeline2dSettings());
        }
    }

    private Path getPipelinePath(CVPipelineSettings setting) {
        String pipelineName = setting.nickname.replace(' ', '_');
        String prefix = ((setting instanceof CVPipeline2dSettings) ? CVPipeline2DPrefix : CVPipeline3DPrefix) + "-";
        String fullFileName = prefix + pipelineName + ".json";
        return Path.of(cameraConfig.getPipelineFolderPath().toString(), fullFileName);
    }

    private boolean pipelineExists(CVPipelineSettings setting) {
        return Files.exists(getPipelinePath(setting));
    }

    public void save(CVPipelineSettings settings) {

        var path = getPipelinePath(settings);

        if (settings instanceof CVPipeline3dSettings) {
            try {
                JacksonHelper.serializer(path, settings);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (settings instanceof CVPipeline2dSettings) {
            try {
                JacksonHelper.serializer(path, settings);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new RuntimeException("saving non-2d and non-3d pipelines not implemented~");
        }
    }

    public void save(List<CVPipelineSettings> settings) {
        for(CVPipelineSettings setting : settings) {
            save(setting);
        }
    }

    public void delete(CVPipelineSettings setting) {
        if(pipelineExists(setting)) {
            try {
                Files.delete(getPipelinePath(setting));
            } catch (IOException e) {
                System.err.println("Failed to delete pipeline!");
            }
        }
    }

    public CVPipelineSettings rename(CVPipelineSettings setting, String newName) {
        if (pipelineExists(setting)) {
            delete(setting);
            setting.nickname = newName;
            save(setting);
        } else {
            setting.nickname = newName;
            save(setting);
        }
        return setting;
    }

    public List<CVPipelineSettings> load() {
        check(); // TODO: this ensures there will be a default pipeline. is the check later necessary?

        File[] pipelineFiles = getPipelineFiles();
        List<CVPipelineSettings> deserializedList = new ArrayList<>();

        if(pipelineFiles == null || pipelineFiles.length < 1) {
            // TODO handle no pipelines to load
            System.err.println("no pipes to load! loading default");
        } else {
            for(File pipelineFile : pipelineFiles) {
                var name = pipelineFile.getName();
                if(name.startsWith(CVPipeline3DPrefix)) {
                    // try to load 3d pipe
                    try {
                        var pipe = JacksonHelper.deserializer(Paths.get(pipelineFile.getPath()), CVPipeline3dSettings.class);
                        deserializedList.add(pipe);
                    } catch (IOException e) {
                        System.err.println("couldn't load cvpipeline3d");
                    }
                } else if(name.startsWith(CVPipeline2DPrefix)) {
                    // try to load 2d pipe
                    try {
                        var pipe = JacksonHelper.deserializer(Paths.get(pipelineFile.getPath()), CVPipeline2dSettings.class);
                        deserializedList.add(pipe);
                    } catch (IOException e) {
                        System.err.println("couldn't load cvpipeline2d");
                    }
                }
            }
        }

        return deserializedList;
    }
}
