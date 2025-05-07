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
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.photonvision.common.configuration.NeuralNetworkPropertyManager.ModelProperties;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.objects.Model;
import org.photonvision.vision.objects.RknnModel;

/**
 * Manages the loading of neural network models.
 *
 * <p>Models are loaded from the filesystem at the <code>modelsFolder</code> location. PhotonVision
 * also supports shipping pre-trained models as resources in the JAR. If the model has already been
 * extracted to the filesystem, it will not be extracted again.
 *
 * <p>Each model must have a corresponding {@link ModelProperties} entry in {@link
 * NeuralNetworkPropertyManager}.
 */
public class NeuralNetworkModelManager {
    /** Singleton instance of the NeuralNetworkModelManager */
    private static NeuralNetworkModelManager INSTANCE;

    /**
     * This function stores the properties of the shipped object detection models. It is stored as a
     * function so that it can be dynamic, to adjust for the models directory.
     */
    private NeuralNetworkPropertyManager getShippedProperties(File modelsDirectory) {
        NeuralNetworkPropertyManager nnProps = new NeuralNetworkPropertyManager();

        nnProps.addModelProperties(
                new ModelProperties(
                        Path.of(modelsDirectory.getAbsolutePath(), "algaeV1-640-640-yolov8n.rknn"),
                        "Algae v8n",
                        new LinkedList<String>(List.of("Algae")),
                        640,
                        480,
                        Family.RKNN,
                        Version.YOLOV8));

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

    public enum Version {
        YOLOV5,
        YOLOV8,
        YOLOV11
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
     *     simple entry containing the UID and nickname
     */
    public HashMap<String, ArrayList<SimpleEntry<String, String>>> getModels() {
        HashMap<String, ArrayList<SimpleEntry<String, String>>> modelMap = new HashMap<>();
        if (models == null) {
            return modelMap;
        }

        models.forEach(
                (backend, backendModels) -> {
                    ArrayList<SimpleEntry<String, String>> modelNames = new ArrayList<>();
                    backendModels.forEach(
                            model -> modelNames.add(new SimpleEntry<>(model.getUID(), model.getNickname())));
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
     * @param modelUID the unique identifier of the model to retrieve
     * @return an Optional containing the model if found, or an empty Optional if not found
     */
    public Optional<Model> getModel(String modelUID) {
        if (models == null) {
            return Optional.empty();
        }

        // Check if the model exists in any supported backend
        for (Family backend : supportedBackends) {
            if (models.containsKey(backend)) {
                Optional<Model> model =
                        models.get(backend).stream().filter(m -> m.getUID().equals(modelUID)).findFirst();
                if (model.isPresent()) {
                    return model;
                }
            }
        }

        return Optional.empty();
    }

    /** The default model when no model is specified. */
    public Optional<Model> getDefaultModel() {
        if (models == null || supportedBackends.isEmpty()) {
            return Optional.empty();
        }

        return models.get(supportedBackends.get(0)).stream().findFirst();
    }

    // Do checking later on, when we create the rknn model
    private void loadModel(ModelProperties properties) {
        if (models == null) {
            models = new HashMap<>();
        }

        if (properties == null) {
            logger.error(
                    "Model properties are null, this could mean the models config was unable to be found in the database");
            return;
        }

        if (!supportedBackends.contains(properties.family())) {
            logger.warn(
                    "Model "
                            + properties.nickname()
                            + " has an unknown extension or is not supported on this hardware.");
            return;
        }

        if (!models.containsKey(properties.family())) {
            models.put(properties.family(), new ArrayList<>());
        }

        try {
            switch (properties.family()) {
                case RKNN -> {
                    models.get(properties.family()).add(new RknnModel(properties));
                    logger.info(
                            "Loaded model "
                                    + properties.nickname()
                                    + " for backend "
                                    + properties.family().toString());
                }
            }
        } catch (IllegalArgumentException e) {
            logger.error("Failed to load model " + properties.nickname(), e);
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

        try {
            Files.walk(modelsDirectory.toPath())
                    .filter(Files::isRegularFile)
                    .forEach(
                            path ->
                                    loadModel(
                                            ConfigManager.getInstance()
                                                    .getConfig()
                                                    .neuralNetworkPropertyManager()
                                                    .getModel(path)));
        } catch (IOException e) {
            logger.error("Failed to discover models at " + modelsDirectory.getAbsolutePath(), e);
        }

        // After loading all of the models, sort them by name to ensure a consistent
        // ordering
        models.forEach(
                (backend, backendModels) -> backendModels.sort((a, b) -> a.getUID().compareTo(b.getUID())));

        // Log
        StringBuilder sb = new StringBuilder();
        sb.append("Discovered models: ");
        models.forEach(
                (backend, backendModels) -> {
                    sb.append(backend).append(" [");
                    backendModels.forEach(model -> sb.append(model.getUID()).append(", "));
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
                                .sum(ConfigManager.getInstance().getConfig().neuralNetworkPropertyManager()));
    }
}
