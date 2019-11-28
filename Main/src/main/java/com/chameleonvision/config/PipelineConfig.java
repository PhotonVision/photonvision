package com.chameleonvision.config;

import com.chameleonvision.util.JacksonHelper;
import com.chameleonvision.vision.pipeline.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PipelineConfig {

    public static final String CVPipeline2DPrefix = "CV2D";
    public static final String CVPipeline3DPrefix = "CV3D";

    private final CameraConfig cameraConfig;

    /**
     * Construct a new PipelineConfig
     * @param cameraConfig the CameraConfig (parent folder, kinda?)
     */
    public PipelineConfig(CameraConfig cameraConfig) {
        this.cameraConfig = cameraConfig;
    }

    void check() {
        cameraConfig.checkFolder();
        // Check if there's at least one pipe
        if (!pipelinesExists()) {
            save(new CVPipeline2dSettings());
        }
    }

    private boolean pipelinesExists() {
        cameraConfig.checkFolder();
        //noinspection ResultOfMethodCallIgnored
        (new File(cameraConfig.getPipelineFolderPath().toUri())).mkdirs();
        var folderContents = new File(cameraConfig.getPipelineFolderPath().toUri()).listFiles();
        if(folderContents == null) return false;
        return cameraConfig.getConfigFolderExists() && folderContents.length > 0;
    }

    private void save(CVPipelineSettings settings) {

        if (settings instanceof CVPipeline3dSettings) {
            Path settingJsonPath = Paths.get(cameraConfig.getPipelineFolderPath().toString(),
                    CVPipeline3DPrefix + settings.nickname.replace(' ', '_') + ".json");
            try {
                JacksonHelper.serializer(settingJsonPath, settings);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (settings instanceof CVPipeline2dSettings) {
            Path settingJsonPath = Paths.get(cameraConfig.getPipelineFolderPath().toString(),
                    CVPipeline2DPrefix + settings.nickname.replace(' ', '_') + ".json");
            try {
                JacksonHelper.serializer(settingJsonPath, settings);
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

    public List<CVPipelineSettings> load() {
        check();
        var pipelineDir = new File(cameraConfig.getPipelineFolderPath().toUri());

        File[] files = pipelineDir.listFiles();
        List<CVPipelineSettings> deserializedList = new ArrayList<>();
        if(files == null || files.length < 1) {
            // TODO handle no pipelines to load
            System.err.println("no pipes to load! loading default");
        } else {
            for(File file : files) {
                var name = file.getName();
                if(name.startsWith(CVPipeline3DPrefix)) {
                    // try to load 3d pipe
                    try {
                        var pipe = JacksonHelper.deserializer(Paths.get(file.getPath()), CVPipeline3dSettings.class);
                        deserializedList.add(pipe);
                    } catch (IOException e) {
                        System.err.println("couldn't load cvpipeline3d");
                    }
                } else if(name.startsWith(CVPipeline2DPrefix)) {
                    // try to load 2d pipe
                    try {
                        var pipe = JacksonHelper.deserializer(Paths.get(file.getPath()), CVPipeline2dSettings.class);
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
