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
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Size;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.rknn.RknnJNI;

/**
 * Manages the loading of neural network models.
 *
 * <p>Models are loaded from the filesystem at the <code>modelsFolder</code> location. PhotonVision
 * also supports shipping pre-trained models as resources in the JAR. If the model has already been
 * extracted to the filesystem, it will not be extracted again.
 *
 * <p>Each model must have a corresponding <code>labels</code> file. The labels file format is
 * simply a list of string names per label, one label per line. The labels file must have the same
 * name as the model file, but with the suffix <code>-labels.txt</code> instead of <code>.rknn
 * </code>.
 *
 * <p>Note: PhotonVision currently only supports YOLOv5 and YOLOv8 models in the <code>.rknn</code>
 * format.
 */
public class NeuralNetworkModelManager {
    /** Singleton instance of the NeuralNetworkModelManager */
    private static NeuralNetworkModelManager INSTANCE;

    /**
     * Private constructor to prevent instantiation
     *
     * @return The NeuralNetworkModelManager instance
     */
    private NeuralNetworkModelManager() {}

    /**
     * Returns the singleton instance of the NeuralNetworkModelManager
     *
     * @return The singleton instance
     */
    public static NeuralNetworkModelManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NeuralNetworkModelManager();
        }
        return INSTANCE;
    }

    /** Logger for the NeuralNetworkModelManager */
    private static final Logger logger = new Logger(NeuralNetworkModelManager.class, LogGroup.Config);

    /**
     * Determines the model version based on the model's filename.
     *
     * <p>"yolov5" -> "YOLO_V5"
     *
     * <p>"yolov8" -> "YOLO_V8"
     *
     * @param modelName The model's filename
     * @return The model version
     */
    private static RknnJNI.ModelVersion getModelVersion(String modelName)
            throws IllegalArgumentException {
        if (modelName.contains("yolov5")) {
            return RknnJNI.ModelVersion.YOLO_V5;
        } else if (modelName.contains("yolov8")) {
            return RknnJNI.ModelVersion.YOLO_V8;
        } else {
            throw new IllegalArgumentException("Unknown model version for model " + modelName);
        }
    }

    /** This class represents a model that can be loaded by the RknnJNI. */
    public class Model {
        public final File modelFile;
        public final RknnJNI.ModelVersion version;
        public final List<String> labels;
        public final Size inputSize;

        /**
         * Model constructor.
         *
         * @param model format `name-width-height-model.format`
         * @param labels
         * @throws IllegalArgumentException
         */
        public Model(String model, String labels) throws IllegalArgumentException {
            this.modelFile = new File(model);

            String[] parts = modelFile.getName().split("-");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Invalid model file name: " + model);
            }

            // TODO: model 'version' need to be replaced the by the product of 'Version' x 'Format'
            this.version = getModelVersion(parts[3]);

            int width = Integer.parseInt(parts[1]);
            int height = Integer.parseInt(parts[2]);
            this.inputSize = new Size(width, height);

            try {
                this.labels = Files.readAllLines(Paths.get(labels));
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read labels file " + labels, e);
            }

            logger.info("Loaded model " + modelFile.getName());
        }

        public String getName() {
            return modelFile.getName();
        }
    }

    /**
     * Stores model information, such as the model file, labels, and version.
     *
     * <p>The first model in the list is the default model.
     */
    private List<Model> models;

    /**
     * Returns the default rknn model. This is simply the first model in the list.
     *
     * @return The default model
     */
    public Model getDefaultRknnModel() {
        return models.get(0);
    }

    /**
     * Enumerates the names of all models.
     *
     * @return A list of model names
     */
    public List<String> getModels() {
        return models.stream().map(model -> model.getName()).toList();
    }

    /**
     * Returns the model with the given name.
     *
     * <p>TODO: Java 17 This should return an Optional Model instead of null.
     *
     * @param modelName The model name
     * @return The model
     */
    public Model getModel(String modelName) {
        Model m =
                models.stream().filter(model -> model.getName().equals(modelName)).findFirst().orElse(null);

        if (m == null) {
            logger.error("Model " + modelName + " not found.");
        }

        return m;
    }

    /**
     * Loads models from the specified folder.
     *
     * @param modelsFolder The folder where the models are stored
     */
    public void loadModels(File modelsFolder) {
        if (!modelsFolder.exists()) {
            logger.error("Models folder " + modelsFolder.getAbsolutePath() + " does not exist.");
            return;
        }

        if (models == null) {
            models = new ArrayList<>();
        }

        try {
            Files.walk(modelsFolder.toPath())
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".rknn"))
                    .forEach(
                            modelPath -> {
                                String model = modelPath.toString();
                                String labels = model.replace(".rknn", "-labels.txt");

                                try {
                                    models.add(new Model(model, labels));
                                } catch (IllegalArgumentException e) {
                                    logger.error("Failed to load model " + model, e);
                                }
                            });
        } catch (IOException e) {
            logger.error("Failed to load models from " + modelsFolder.getAbsolutePath(), e);
        }

        // Log the loaded models
        StringBuilder sb = new StringBuilder();
        sb.append("Loaded models: ");
        for (Model model : models) {
            sb.append(model.modelFile.getName()).append(", ");
        }
        sb.setLength(sb.length() - 2);
        logger.info(sb.toString());
    }

    /**
     * Extracts models from a JAR resource and copies them to the specified folder.
     *
     * @param modelsFolder the folder where the models will be copied to
     */
    public void extractModels(File modelsFolder) {
        if (!modelsFolder.exists()) {
            modelsFolder.mkdirs();
        }

        String resourcePath = "models";
        try {
            URL resourceURL = NeuralNetworkModelManager.class.getClassLoader().getResource(resourcePath);
            if (resourceURL == null) {
                logger.error("Failed to find jar resource at " + resourcePath);
                return;
            }

            Path resourcePathResolved = Paths.get(resourceURL.toURI());
            Files.walk(resourcePathResolved)
                    .forEach(sourcePath -> copyResource(sourcePath, resourcePathResolved, modelsFolder));
        } catch (Exception e) {
            logger.error("Failed to extract models from JAR", e);
        }
    }

    /**
     * Copies a resource from the source path to the target path.
     *
     * @param sourcePath The path of the resource to be copied.
     * @param resourcePathResolved The resolved path of the resource.
     * @param modelsFolder The folder where the resource will be copied to.
     */
    private void copyResource(Path sourcePath, Path resourcePathResolved, File modelsFolder) {
        Path targetPath =
                Paths.get(
                        modelsFolder.getAbsolutePath(), resourcePathResolved.relativize(sourcePath).toString());
        try {
            if (Files.isDirectory(sourcePath)) {
                Files.createDirectories(targetPath);
            } else {
                Path parentDir = targetPath.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }

                if (!Files.exists(targetPath)) {
                    Files.copy(sourcePath, targetPath);
                } else {
                    long sourceSize = Files.size(sourcePath);
                    long targetSize = Files.size(targetPath);
                    if (sourceSize != targetSize) {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to copy " + sourcePath + " to " + targetPath, e);
        }
    }
}
