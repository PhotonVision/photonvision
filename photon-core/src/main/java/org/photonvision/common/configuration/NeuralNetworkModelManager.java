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

package org.photonvision.common.configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class NeuralNetworkModelManager {
    private static NeuralNetworkModelManager INSTANCE;
    private static final Logger logger = new Logger(NeuralNetworkModelManager.class, LogGroup.Config);

    private final String MODEL_NAME = "note-640-640-yolov5s.rknn";
    private File defaultModelFile;
    private List<String> labels;

    public static NeuralNetworkModelManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NeuralNetworkModelManager();
        }
        return INSTANCE;
    }

    /**
     * Perform initial setup and extract default model from JAR to the filesystem
     *
     * @param modelsFolder Where models live
     */
    public void initialize(File modelsFolder) {
        var modelResourcePath = "/models/" + MODEL_NAME;
        this.defaultModelFile = new File(modelsFolder, MODEL_NAME);
        extractResource(modelResourcePath, defaultModelFile);

        File labelsFile = new File(modelsFolder, "labels.txt");
        var labelResourcePath = "/models/" + labelsFile.getName();
        extractResource(labelResourcePath, labelsFile);

        try {
            labels = Files.readAllLines(Paths.get(labelsFile.getPath()));
        } catch (IOException e) {
            logger.error("Error reading labels.txt", e);
        }
    }

    private void extractResource(String resourcePath, File outputFile) {
        try (var in = NeuralNetworkModelManager.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                logger.error("Failed to find jar resource at " + resourcePath);
                return;
            }

            if (!outputFile.exists()) {
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    int read = -1;
                    byte[] buffer = new byte[1024];
                    while ((read = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                } catch (IOException e) {
                    logger.error("Error extracting resource to " + outputFile.toPath().toString(), e);
                }
            } else {
                logger.info(
                        "File " + outputFile.toPath().toString() + " already exists. Skipping extraction.");
            }
        } catch (IOException e) {
            logger.error("Error finding jar resource " + resourcePath, e);
        }
    }

    public File getDefaultRknnModel() {
        return defaultModelFile;
    }

    public List<String> getLabels() {
        return labels;
    }
}
