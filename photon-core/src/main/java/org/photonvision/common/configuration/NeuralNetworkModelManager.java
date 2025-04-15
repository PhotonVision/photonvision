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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.photonvision.common.configuration.NeuralNetworkProperties.RknnModelProperties;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.rknn.RknnJNI.ModelVersion;
import org.photonvision.vision.objects.Model;
import org.photonvision.vision.objects.RknnModel;

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
 */
public class NeuralNetworkModelManager {
    /** Singleton instance of the NeuralNetworkModelManager */
    private static NeuralNetworkModelManager INSTANCE;

    /**
     * This function stores the properties of the shipped object detection models. It is stored as a
     * function so that it can be dynamic, to adjust for the models directory.
     */
    private NeuralNetworkProperties getShippedProperties(File modelsDirectory) {
        NeuralNetworkProperties nnProps = new NeuralNetworkProperties();

        nnProps.addModelProperties(
                nnProps
                .new RknnModelProperties(
                        Path.of(modelsDirectory.getAbsolutePath(), "NAMEHERE.rknn"),
                        "foo",
                        new LinkedList<String>(),
                        0,
                        0,
                        Family.RKNN,
                        ModelVersion.YOLO_V8));

        return nnProps;
    }

    /**
     * Private constructor to prevent instantiation
     *
     * @return The NeuralNetworkModelManager instance
     */
    private NeuralNetworkModelManager() {
        ArrayList<Family> backends = new ArrayList<>();

        if (Platform.isRK3588()) {
            backends.add(Family.RKNN);
        }

        supportedBackends = backends;
    }

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

    public enum Family {
        RKNN
    }

    private final List<Family> supportedBackends;

    /**
     * Retrieves the list of supported backends.
     *
     * @return the list
     */
    public List<String> getSupportedBackends() {
        return supportedBackends.stream().map(Enum::toString).toList();
    }

    /**
     * Stores model information, such as the model file, labels, and version.
     *
     * <p>The first model in the list is the default model.
     */
    private Map<Family, ArrayList<Model>> models;

    /**
     * Retrieves the deep neural network models available, in a format that can be used by the
     * frontend.
     *
     * @return A map containing the available models, where the key is the backend and the value is a
     *     list of model names.
     */
    public HashMap<String, ArrayList<String>> getModels() {
        HashMap<String, ArrayList<String>> modelMap = new HashMap<>();
        if (models == null) {
            return modelMap;
        }

        models.forEach(
                (backend, backendModels) -> {
                    ArrayList<String> modelNames = new ArrayList<>();
                    backendModels.forEach(model -> modelNames.add(model.getName()));
                    modelMap.put(backend.toString(), modelNames);
                });

        return modelMap;
    }

    /**
     * Retrieves the model with the specified name, assuming it is available under a supported
     * backend.
     *
     * <p>If this method returns `Optional.of(..)` then the model should be safe to load.
     *
     * @param modelName the name of the model to retrieve
     * @return an Optional containing the model if found, or an empty Optional if not found
     */
    public Optional<Model> getModel(String modelName) {
        if (models == null) {
            return Optional.empty();
        }

        // Check if the model exists in any supported backend
        for (Family backend : supportedBackends) {
            if (models.containsKey(backend)) {
                Optional<Model> model =
                        models.get(backend).stream().filter(m -> m.getName().equals(modelName)).findFirst();
                if (model.isPresent()) {
                    return model;
                }
            }
        }

        return Optional.empty();
    }

    /** The default model when no model is specified. */
    public Optional<Model> getDefaultModel() {
        if (models == null) {
            return Optional.empty();
        }

        if (supportedBackends.isEmpty()) {
            return Optional.empty();
        }

        return models.get(supportedBackends.get(0)).stream().findFirst();
    }

    // Do checking later on, when we create the rknn model
    private void loadModel(RknnModelProperties properties) {
        if (models == null) {
            models = new HashMap<>();
        }

        if (properties == null) {
            logger.error(
                    "Model properties are null, this could mean the models config was unable to be found in the database");
            return;
        }

        if (!supportedBackends.contains(properties.family)) {
            logger.warn(
                    "Model "
                            + properties.nickname
                            + " has an unknown extension or is not supported on this hardware.");
            return;
        }

        if (!models.containsKey(properties.family)) {
            models.put(properties.family, new ArrayList<>());
        }

        try {
            switch (properties.family) {
                case RKNN -> {
                    models.get(properties.family).add(new RknnModel(properties));
                    logger.info(
                            "Loaded model "
                                    + properties.nickname
                                    + " for backend "
                                    + properties.family.toString());
                }
            }
        } catch (IllegalArgumentException e) {
            logger.error("Failed to load model " + properties.nickname, e);
        }
    }

    /**
     * Discovers DNN models from the specified folder.
     *
     * <p>This makes the assumption that all of the models have their properties stored in the
     * database
     */
    public void discoverModels() {
        logger.info("Supported backends: " + supportedBackends);

        File modelsDirectory = ConfigManager.getInstance().getModelsDirectory();

        if (!modelsDirectory.exists()) {
            logger.error("Models folder " + modelsDirectory.getAbsolutePath() + " does not exist.");
            return;
        }

        models = new HashMap<>();

        // TODO: Load neural network properties from json/create new one
        try {
            Files.walk(modelsDirectory.toPath())
                    .filter(Files::isRegularFile)
                    .forEach(
                            path ->
                                    loadModel(
                                            ConfigManager.getInstance()
                                                    .getConfig()
                                                    .getNeuralNetworkProperties()
                                                    .getModelProperties(path)));
        } catch (IOException e) {
            logger.error("Failed to discover models at " + modelsDirectory.getAbsolutePath(), e);
        }

        // After loading all of the models, sort them by name to ensure a consistent
        // ordering
        models.forEach(
                (backend, backendModels) ->
                        backendModels.sort((a, b) -> a.getName().compareTo(b.getName())));

        // Log
        StringBuilder sb = new StringBuilder();
        sb.append("Discovered models: ");
        models.forEach(
                (backend, backendModels) -> {
                    sb.append(backend).append(" [");
                    backendModels.forEach(model -> sb.append(model.getName()).append(", "));
                    sb.append("] ");
                });
    }

    /**
     * Extracts models from the JAR and copies them to disk. Also copies properties into the database.
     */
    public void extractModels() {
        File modelsDirectory = ConfigManager.getInstance().getModelsDirectory();

        if (!modelsDirectory.exists() && !modelsDirectory.mkdirs()) {
            throw new RuntimeException("Failed to create directory: " + modelsDirectory);
        }

        String resource = "models";

        try {
            String jarPath =
                    getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            try (JarFile jarFile = new JarFile(jarPath)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.getName().startsWith(resource + "/") || entry.isDirectory()) {
                        continue;
                    }
                    Path outputPath =
                            modelsDirectory.toPath().resolve(entry.getName().substring(resource.length() + 1));

                    if (Files.exists(outputPath)) {
                        logger.info("Skipping extraction of DNN resource: " + entry.getName());
                        continue;
                    }

                    Files.createDirectories(outputPath.getParent());
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
                        logger.info("Extracted DNN resource: " + entry.getName());
                    } catch (IOException e) {
                        logger.error("Failed to extract DNN resource: " + entry.getName(), e);
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            logger.error("Error extracting models", e);
        }

        ConfigManager.getInstance()
                .getConfig()
                .setNeuralNetworkProperties(
                        getShippedProperties(modelsDirectory)
                                .add(ConfigManager.getInstance().getConfig().getNeuralNetworkProperties()));
    }
}
