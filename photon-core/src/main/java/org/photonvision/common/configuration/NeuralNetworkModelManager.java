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
import java.util.stream.Stream;
import org.photonvision.common.configuration.NeuralNetworkPropertyManager.ModelProperties;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.objects.Model;
import org.photonvision.vision.objects.RknnModel;
import org.photonvision.vision.objects.RubikModel;

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

    private final List<Family> supportedBackends = new ArrayList<>();

    /**
     * This function stores the properties of the shipped object detection models. It is stored as a
     * function so that it can be dynamic, to adjust for the models directory.
     */
    private NeuralNetworkPropertyManager getShippedProperties(File modelsDirectory) {
        NeuralNetworkPropertyManager nnProps = new NeuralNetworkPropertyManager();

        LinkedList<String> cocoLabels =
                new LinkedList<String>(
                        List.of(
                                "person",
                                "bicycle",
                                "car",
                                "motorcycle",
                                "airplane",
                                "bus",
                                "train",
                                "truck",
                                "boat",
                                "traffic light",
                                "fire hydrant",
                                "stop sign",
                                "parking meter",
                                "bench",
                                "bird",
                                "cat",
                                "dog",
                                "horse",
                                "sheep",
                                "cow",
                                "elephant",
                                "bear",
                                "zebra",
                                "giraffe",
                                "backpack",
                                "umbrella",
                                "handbag",
                                "tie",
                                "suitcase",
                                "frisbee",
                                "skis",
                                "snowboard",
                                "sports ball",
                                "kite",
                                "baseball bat",
                                "baseball glove",
                                "skateboard",
                                "surfboard",
                                "tennis racket",
                                "bottle",
                                "wine glass",
                                "cup",
                                "fork",
                                "knife",
                                "spoon",
                                "bowl",
                                "banana",
                                "apple",
                                "sandwich",
                                "orange",
                                "broccoli",
                                "carrot",
                                "hot dog",
                                "pizza",
                                "donut",
                                "cake",
                                "chair",
                                "couch",
                                "potted plant",
                                "bed",
                                "dining table",
                                "toilet",
                                "tv",
                                "laptop",
                                "mouse",
                                "remote",
                                "keyboard",
                                "cell phone",
                                "microwave",
                                "oven",
                                "toaster",
                                "sink",
                                "refrigerator",
                                "book",
                                "clock",
                                "vase",
                                "scissors",
                                "teddy bear",
                                "hair drier",
                                "toothbrush"));

        nnProps.addModelProperties(
                new ModelProperties(
                        Path.of(modelsDirectory.getAbsolutePath(), "algaeV1-640-640-yolov8n.rknn"),
                        "Algae v8n",
                        new LinkedList<String>(List.of("Algae")),
                        640,
                        480,
                        Family.RKNN,
                        Version.YOLOV8));

        nnProps.addModelProperties(
                new ModelProperties(
                        Path.of(modelsDirectory.getAbsolutePath(), "yolov8nCOCO.rknn"),
                        "COCO",
                        cocoLabels,
                        640,
                        640,
                        Family.RKNN,
                        Version.YOLOV8));

        nnProps.addModelProperties(
                new ModelProperties(
                        Path.of(modelsDirectory.getAbsolutePath(), "algae-coral-yolov8s.tflite"),
                        "Algae Coral v8s",
                        new LinkedList<String>(List.of("Algae", "Coral")),
                        640,
                        640,
                        Family.RUBIK,
                        Version.YOLOV8));

        nnProps.addModelProperties(
                new ModelProperties(
                        Path.of(modelsDirectory.getAbsolutePath(), "yolov8nCOCO.tflite"),
                        "COCO",
                        cocoLabels,
                        640,
                        640,
                        Family.RUBIK,
                        Version.YOLOV8));

        return nnProps;
    }

    /**
     * Private constructor to prevent instantiation
     *
     * @return The NeuralNetworkModelManager instance
     */
    private NeuralNetworkModelManager() {
        switch (Platform.getCurrentPlatform()) {
            case LINUX_QCS6490 -> supportedBackends.add(Family.RUBIK);
            case LINUX_RK3588_64 -> supportedBackends.add(Family.RKNN);
            default -> {
                logger.warn(
                        "No supported neural network backends found for this platform: "
                                + Platform.getCurrentPlatform());
                // No supported backends, so we won't load any models
                return;
            }
        }
    }

    /**
     * Returns the singleton instance of the NeuralNetworkModelManager. Call getInstance() to use the
     * default (no reset), or getInstance(true) to reset.
     *
     * @return The singleton instance
     */
    public static NeuralNetworkModelManager getInstance() {
        return getInstance(false);
    }

    /**
     * Returns the singleton instance of the NeuralNetworkModelManager, optionally resetting it.
     *
     * @param reset If true, resets the instance
     * @return The singleton instance
     */
    public static NeuralNetworkModelManager getInstance(boolean reset) {
        if (INSTANCE == null || reset) {
            INSTANCE = new NeuralNetworkModelManager();
        }
        return INSTANCE;
    }

    /** Logger for the NeuralNetworkModelManager */
    private static final Logger logger = new Logger(NeuralNetworkModelManager.class, LogGroup.Config);

    public enum Family {
        RKNN(".rknn"),
        RUBIK(".tflite");

        private final String fileExtension;

        private Family(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        public String extension() {
            return fileExtension;
        }
    }

    public enum Version {
        YOLOV5,
        YOLOV8,
        YOLOV11
    }

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

    // Do checking later on, when we create the model object
    private void loadModel(Path path) {
        if (models == null) {
            models = new HashMap<>();
        }

        ModelProperties properties =
                ConfigManager.getInstance().getConfig().neuralNetworkPropertyManager().getModel(path);

        if (properties == null) {
            logger.error(
                    "Model properties are null. This could mean the config for model "
                            + path
                            + " was unable to be found in the database.");
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
                }
                case RUBIK -> {
                    models.get(properties.family()).add(new RubikModel(properties));
                }
            }
            logger.info(
                    "Loaded model "
                            + properties.nickname()
                            + " for backend "
                            + properties.family().toString());
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

        try (Stream<Path> files = Files.walk(modelsDirectory.toPath())) {
            files
                    .filter(Files::isRegularFile)
                    .filter(
                            path ->
                                    supportedBackends.stream()
                                            .anyMatch(family -> path.toString().endsWith(family.extension())))
                    .forEach(this::loadModel);
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

        // Filter shippedProprties by supportedBackends
        NeuralNetworkPropertyManager supportedProperties = new NeuralNetworkPropertyManager();
        for (ModelProperties model : getShippedProperties(modelsDirectory).getModels()) {
            if (supportedBackends.contains(model.family())) {
                supportedProperties.addModelProperties(model);
                logger.debug("Added shipped model: " + model.modelPath().getFileName().toString());
            }
        }

        // Used for checking if the model to be extracted is supported for this architecture
        ArrayList<String> supportedModelFileNames = new ArrayList<String>();
        for (ModelProperties model : supportedProperties.getModels()) {
            supportedModelFileNames.add(model.modelPath().getFileName().toString());
        }

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

                    // Check if the file already exists or if it is a supported model file
                    if ((Files.exists(outputPath))
                            || !(entry.getName().endsWith("txt")
                                    || supportedModelFileNames.contains(
                                            entry.getName().substring(entry.getName().lastIndexOf('/') + 1)))) {
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

        // Combine with existing properties
        ConfigManager.getInstance()
                .getConfig()
                .setNeuralNetworkProperties(
                        supportedProperties.sum(
                                ConfigManager.getInstance().getConfig().neuralNetworkPropertyManager()));
    }

    public boolean clearModels() {
        File modelsDirectory = ConfigManager.getInstance().getModelsDirectory();

        if (modelsDirectory.exists()) {
            try (Stream<Path> files = Files.walk(modelsDirectory.toPath())) {
                files
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(
                                path -> {
                                    try {
                                        Files.delete(path);
                                    } catch (IOException e) {
                                        logger.error("Failed to delete file: " + path, e);
                                    }
                                });
            } catch (IOException e) {
                logger.error("Failed to delete models directory", e);
                return false;
            }
        }

        // Delete model info
        return ConfigManager.getInstance().getConfig().neuralNetworkPropertyManager().clear();
    }

    public File exportSingleModel(String modelPath) {
        try {
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                logger.error("Model file does not exist: " + modelFile.getAbsolutePath());
                return null;
            }

            ModelProperties properties =
                    ConfigManager.getInstance()
                            .getConfig()
                            .neuralNetworkPropertyManager()
                            .getModel(Path.of(modelPath));

            String fileName = "";
            String suffix = modelFile.getName().substring(modelFile.getName().lastIndexOf('.'));
            if (properties != null) {
                fileName =
                        String.format(
                                "%s-%s-%s-%dx%d-%s",
                                properties.nickname().replace(" ", ""),
                                properties.family(),
                                properties.version(),
                                properties.resolutionWidth(),
                                properties.resolutionHeight(),
                                String.join("_", properties.labels()));
            } else {
                fileName = new File(modelPath).getName();
            }

            try {
                var out = Files.createTempFile(fileName, suffix);
                Files.copy(
                        modelFile.toPath(),
                        out,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
                return out.toFile();
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Failed to export model file: " + modelFile.getAbsolutePath(), e);
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to export model file: " + modelPath, e);
            return null;
        }
    }
}
