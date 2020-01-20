package com.chameleonvision.config;

import com.chameleonvision.config.serializers.StandardCVPipelineSettingsDeserializer;
import com.chameleonvision.config.serializers.StandardCVPipelineSettingsSerializer;
import com.chameleonvision.util.FileHelper;
import com.chameleonvision.util.JacksonHelper;
import com.chameleonvision.vision.pipeline.*;
import com.chameleonvision.vision.pipeline.impl.StandardCVPipelineSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PipelineConfig {

    private final CameraConfig cameraConfig;

    /**
     * Construct a new PipelineConfig
     * @param cameraConfig the CameraConfig (parent folder, kinda?)
     */
    PipelineConfig(CameraConfig cameraConfig) {
        this.cameraConfig = cameraConfig;
    }

    private void checkFolder() {
        if ( !(new File(cameraConfig.pipelineFolderPath.toUri()).mkdirs())) {
            if (Files.notExists(cameraConfig.pipelineFolderPath)) {
                System.err.println("Failed to create pipelines folder.");
            }
        }
        try {
            FileHelper.setFilePerms(cameraConfig.pipelineFolderPath);
        } catch (IOException e) {
            // ignored
        }
    }

    private File[] getPipelineFiles() {
        return new File(cameraConfig.pipelineFolderPath.toUri()).listFiles();
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
            save(new StandardCVPipelineSettings());
        }
    }

    private Path getPipelinePath(CVPipelineSettings setting) {
        String pipelineName = setting.nickname.replace(' ', '_');
        String fullFileName = pipelineName + ".json";
        return Path.of(cameraConfig.pipelineFolderPath.toString(), fullFileName);
    }

    private boolean pipelineExists(CVPipelineSettings setting) {
        return Files.exists(getPipelinePath(setting));
    }

    public void save(CVPipelineSettings settings) {

        var path = getPipelinePath(settings);

        if (settings instanceof StandardCVPipelineSettings) {
            try {
                JacksonHelper.serialize(path, (StandardCVPipelineSettings)settings, StandardCVPipelineSettings.class, new StandardCVPipelineSettingsSerializer());
                FileHelper.setFilePerms(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                JacksonHelper.serializer(path, settings);
                FileHelper.setFilePerms(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                    try {
                        var pipe = JacksonHelper.deserialize(Paths.get(pipelineFile.getPath()), StandardCVPipelineSettings.class, new StandardCVPipelineSettingsDeserializer());
                        deserializedList.add(pipe);
                    } catch (IOException e) {
                        System.err.println("couldn't load cvpipeline2d");
                    }
            }
        }

        return deserializedList;
    }
}
