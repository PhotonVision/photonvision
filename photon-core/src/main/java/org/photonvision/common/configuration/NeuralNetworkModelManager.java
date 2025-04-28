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

import io.javalin.http.UploadedFile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.model.manager.ModelManager;
import org.photonvision.model.manager.RknnManager;
import org.photonvision.model.vision.Model;

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
     * Private constructor to prevent instantiation
     *
     * @return The NeuralNetworkModelManager instance
     */
    private NeuralNetworkModelManager() {
        // Initialize the list of model managers based on platform
        List<ModelManager> managers = new ArrayList<>();

        if (Platform.isRK3588()) {
            managers.add(new RknnManager());
        }

        this.modelManagers = Collections.unmodifiableList(managers);
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

    // Use a list of model managers instead of the enum
    private final List<ModelManager> modelManagers;

    /**
     * Retrieves the list of supported backend format information.
     *
     * @return the list of backend info objects
     */
    public List<ModelManager.Info> getSupportedBackends() {
        return modelManagers.stream().map(ModelManager::getInfo).toList();
    }

    /**
     * Stores model information, such as the model file, labels, and version.
     *
     * <p>The first model in the list is the default model.
     */
    private Map<String, ArrayList<Model>> models;

    /**
     * Retrieves the deep neural network models available, in a format that can be used by the
     * frontend.
     *
     * @return A map containing the available models, where the key is the backend name and the value
     *     is a list of model names.
     */
    public HashMap<String, ArrayList<String>> getModels() {
        HashMap<String, ArrayList<String>> modelMap = new HashMap<>();
        if (models == null) {
            return modelMap;
        }

        // Use model manager backend name as the key
        models.forEach(
                (backendName, backendModels) -> {
                    ArrayList<String> modelNames = new ArrayList<>();
                    backendModels.forEach(model -> modelNames.add(model.getName()));
                    modelMap.put(backendName, modelNames);
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

        // Iterate through all backend lists in the map
        for (List<Model> backendModels : models.values()) {
            Optional<Model> model =
                    backendModels.stream().filter(m -> m.getName().equals(modelName)).findFirst();
            if (model.isPresent()) {
                return model;
            }
        }

        return Optional.empty();
    }

    /** The default model when no model is specified. */
    public Optional<Model> getDefaultModel() {
        if (models == null || modelManagers.isEmpty() || models.isEmpty()) {
            return Optional.empty();
        }

        ModelManager defaultManager = modelManagers.get(0);
        List<Model> backendModels = models.get(defaultManager.getBackendName());

        if (backendModels == null || backendModels.isEmpty()) {
            Optional<List<Model>> firstList =
                    models.values().stream()
                            .filter(list -> !list.isEmpty())
                            .map(list -> (List<Model>) list)
                            .findFirst();
            if (firstList.isPresent()) {
                backendModels = firstList.get();
            } else {
                return Optional.empty();
            }
        }

        return backendModels.stream().findFirst();
    }

    /**
     * Discovers DNN models from the specified folder.
     *
     * @param modelsDirectory The folder where the models are stored
     */
    public void discoverModels(File modelsDirectory) {
        logger.info("Discovering models in: " + modelsDirectory.getAbsolutePath());
        // Log model managers instead of backends
        logger.info(
                "Using model managers: "
                        + modelManagers.stream()
                                .map(ModelManager::getBackendName)
                                .collect(Collectors.joining(", ")));

        if (!modelsDirectory.exists()) {
            logger.error("Models folder " + modelsDirectory.getAbsolutePath() + " does not exist.");
            this.models = new HashMap<>(); // Ensure models map is initialized
            return;
        }

        Map<String, ArrayList<Model>> discoveredModels = new HashMap<>();
        List<Path> processedPaths = new ArrayList<>();

        try {
            List<Path> allPaths =
                    Files.walk(modelsDirectory.toPath())
                            .filter(path -> !path.equals(modelsDirectory.toPath()))
                            .collect(Collectors.toList());

            // 2. Process paths with each model manager in order of preference
            for (ModelManager manager : modelManagers) {
                String backendName = manager.getBackendName();

                for (Path path : allPaths) {
                    if (processedPaths.contains(path)) {
                        continue;
                    }

                    if (manager.supportsPath(path)) {
                        logger.info("Path " + path + " supported by model manager " + backendName);
                        try {
                            Model model = manager.loadFromPath(path, modelsDirectory);
                            discoveredModels.computeIfAbsent(backendName, k -> new ArrayList<>()).add(model);
                            processedPaths.add(path);

                            logger.info(
                                    "Successfully loaded model: " + model.getName() + " for backend " + backendName);
                        } catch (IOException | IllegalArgumentException e) {
                            logger.error(
                                    "Failed to load model from path " + path + " using model manager " + backendName,
                                    e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to walk models directory: " + modelsDirectory.getAbsolutePath(), e);
        }

        discoveredModels.forEach(
                (backendName, backendModels) -> backendModels.sort(Comparator.comparing(Model::getName)));

        this.models = discoveredModels;

        StringBuilder sb = new StringBuilder();
        sb.append("Discovered models: ");
        if (this.models.isEmpty()) {
            sb.append("None");
        } else {
            this.models.forEach(
                    (backendName, backendModels) -> {
                        sb.append(backendName).append(" [");
                        if (backendModels.isEmpty()) {
                            sb.append("None");
                        } else {
                            backendModels.forEach(model -> sb.append(model.getName()).append(", "));
                            if (!backendModels.isEmpty()) {
                                sb.setLength(sb.length() - 2);
                            }
                        }
                        sb.append("] ");
                    });
        }
        logger.info(sb.toString());
    }

    /**
     * Extracts models from the JAR and copies them to disk.
     *
     * @param modelsDirectory the directory on disk to save models
     */
    public void extractModels(File modelsDirectory) {
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
    }

    /**
     * Handles the uploaded model and labels files: validates them, saves them, and re-discovers
     * models.
     *
     * @param modelFile The uploaded model file (.rknn).
     * @param labelsFile The uploaded labels file (.txt).
     * @param modelsDirectory The directory where models should be saved.
     * @throws IllegalArgumentException If filenames are invalid or don't match conventions.
     * @throws IOException If file validation fails, saving fails, or no suitable backend is found.
     */
    public void handleUpload(UploadedFile modelFile, UploadedFile labelsFile, File modelsDirectory)
            throws IllegalArgumentException, IOException {
        ModelManager manager = null;
        // Iterate through model managers
        for (ModelManager backendManager : modelManagers) {
            if (backendManager.supportsUpload(modelFile.filename(), labelsFile.filename())) {
                manager = backendManager;
                break;
            }
        }

        if (manager == null) {
            String supportedUploadTypes =
                    modelManagers.stream()
                            .map(ModelManager::getUploadAcceptType)
                            .distinct()
                            .collect(Collectors.joining(", "));
            throw new IOException(
                    "Unsupported file pair based on names/extensions. Model: "
                            + modelFile.filename()
                            + ", Labels: "
                            + labelsFile.filename()
                            + ". Expected model types: "
                            + supportedUploadTypes
                            + " with matching -labels.txt");
        }

        logger.info("Handling upload with backend: " + manager.getBackendName());

        // 1. Validate Upload (basic file type check)
        Optional<String> validationError = manager.validateUpload(modelFile, labelsFile);
        if (validationError.isPresent()) {
            throw new IOException("File validation failed: " + validationError.get());
        }

        // 2. Verify Names (strict naming convention check)
        manager.verifyNames(modelFile.filename(), labelsFile.filename());
        logger.info("Filename verification passed for " + modelFile.filename());

        // 3. Save Uploaded Files (copy or unzip)
        manager.saveUploadedFiles(modelFile, labelsFile, modelsDirectory);
        logger.info("Successfully saved files for " + modelFile.filename());

        // 4. Re-discover models after successful upload
        logger.info("Re-discovering models after upload...");
        discoverModels(modelsDirectory);
    }
}
