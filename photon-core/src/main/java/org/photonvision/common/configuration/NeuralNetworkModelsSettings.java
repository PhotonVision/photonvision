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

import io.avaje.jsonb.Json;
import io.avaje.jsonb.JsonType;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.Types;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Family;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Version;

@Json
public class NeuralNetworkModelsSettings {
    /*
     * The properties of the model. This is used to determine which model to load.
     * The only families currently supported are RKNN and Rubik (custom .tflite)
     */
    @Json
    public record ModelProperties(
            Path modelPath,
            String nickname,
            List<String> labels,
            int resolutionWidth,
            int resolutionHeight,
            Family family,
            Version version) {
        ModelProperties(ModelProperties other) {
            this(
                    other.modelPath,
                    other.nickname,
                    other.labels, // note this does not clone the underlying list
                    other.resolutionWidth,
                    other.resolutionHeight,
                    other.family,
                    other.version);
        }

        // ============= Migration code from v2025.3.1 ===========

        private static Pattern modelPattern =
                Pattern.compile("^([a-zA-Z0-9._]+)-(\\d+)-(\\d+)-(yolov(?:5|8|11)[nsmlx]*)\\.rknn$");

        static ModelProperties createFromFilename(String modelFileName)
                throws IllegalArgumentException, IOException {
            // Used to point to default models directory
            var model =
                    ConfigManager.getInstance().getModelsDirectory().toPath().resolve(modelFileName).toFile();

            // Get the model extension and check if it is supported
            String modelExtension = model.getName().substring(model.getName().lastIndexOf('.'));
            if (!modelExtension.equals(".rknn")) {
                throw new IllegalArgumentException("Model " + modelFileName + " is not a supported format");
            }

            var backend =
                    Arrays.stream(NeuralNetworkModelManager.Family.values())
                            .filter(b -> b.extension().equals(modelExtension))
                            .findFirst();

            if (!backend.isPresent()) {
                throw new IllegalArgumentException("Model " + modelFileName + " cannot find backend");
            }

            String labelFile = model.getAbsolutePath().replace(backend.get().extension(), "-labels.txt");
            List<String> labels = Files.readAllLines(Paths.get(labelFile));

            String[] parts = parseRKNNName(modelFileName);
            var version = getModelVersion(parts[3]);
            int width = Integer.parseInt(parts[1]);
            int height = Integer.parseInt(parts[2]);

            return new ModelProperties(
                    model.toPath(),
                    model.getName(),
                    labels,
                    // all files used to be 640x640
                    width,
                    height,
                    Family.RKNN,
                    version);
        }

        /**
         * Determines the model version based on the model's filename.
         *
         * <p>"yolov5" -> "YOLO_V5"
         *
         * <p>"yolov8" -> "YOLO_V8"
         *
         * <p>"yolov11" -> "YOLO_V11"
         *
         * @param modelName The model's filename
         * @return The model version
         */
        private static Version getModelVersion(String modelName) throws IllegalArgumentException {
            if (modelName.contains("yolov5")) {
                return Version.YOLOV5;
            } else if (modelName.contains("yolov8")) {
                return Version.YOLOV8;
            } else if (modelName.contains("yolov11")) {
                return Version.YOLOV11;
            } else {
                throw new IllegalArgumentException("Unknown model version for model " + modelName);
            }
        }

        /**
         * Parse RKNN name and return the name, width, height, and model type.
         *
         * <p>This is static as it is not dependent on the state of the class.
         *
         * @param modelName the name of the model
         * @throws IllegalArgumentException if the model name does not follow the naming convention
         * @return an array containing the name, width, height, and model type
         */
        public static String[] parseRKNNName(String modelName) {
            Matcher modelMatcher = modelPattern.matcher(modelName);

            if (!modelMatcher.matches()) {
                throw new IllegalArgumentException(
                        "Model name must follow the naming convention of name-widthResolution-heightResolution-modelType.rknn");
            }

            return new String[] {
                modelMatcher.group(1), modelMatcher.group(2), modelMatcher.group(3), modelMatcher.group(4)
            };
        }
    }

    // The path to the model is used as the key in the map because it is unique to
    // the model, and should not change
    @Json.Ignore
    private HashMap<Path, ModelProperties> modelPathToProperties =
            new HashMap<Path, ModelProperties>();

    /**
     * Constructor for the NeuralNetworkProperties class.
     *
     * <p>This object holds a HashMap of {@link ModelProperties} objects
     */
    public NeuralNetworkModelsSettings() {}

    /**
     * Constructor for the NeuralNetworkProperties class.
     *
     * <p>This object holds a HashMap of {@link ModelProperties} objects.
     *
     * @param modelPropertiesMap When the class is constructed, it will hold the provided map
     */
    public NeuralNetworkModelsSettings(HashMap<Path, ModelProperties> modelPropertiesMap) {
        modelPathToProperties = modelPropertiesMap;
    }

    /**
     * Constructor for the NeuralNetworkProperties class.
     *
     * <p>This object holds a HashMap of {@link ModelProperties} objects.
     *
     * @param modelPropertiesList When the class is constructed, it will hold the provided list
     */
    @Json.Creator
    public NeuralNetworkModelsSettings(
            ModelProperties[] models, @Json.Unmapped Map<String, Object> unmapped) {
        JsonType<Map<String, ModelProperties>> modelPropsMapJsonb =
                Jsonb.instance().type(Types.mapOf(ModelProperties.class));
        Stream<ModelProperties> modelPropsStream;
        if (models != null) {
            modelPropsStream = Arrays.stream(models);
        } else if (unmapped.containsKey("modelPathToProperties")) {
            // MIGRATION: 2026
            modelPropsStream =
                    modelPropsMapJsonb.fromObject(unmapped.get("modelPathToProperties")).values().stream();
        } else {
            modelPropsStream = Stream.empty();
        }
        this(
                modelPropsStream.collect(
                        Collectors.toMap(
                                (model) -> model.modelPath(),
                                (model) -> model,
                                (prev, next) -> next,
                                HashMap::new)));
    }

    @Override
    public String toString() {
        String toReturn = "";

        toReturn += "NeuralNetworkProperties [";

        toReturn += modelPathToProperties.toString() + "]";

        return toReturn;
    }

    /**
     * Add a model to the list of models.
     *
     * @param modelProperties
     */
    public void addModelProperties(ModelProperties modelProperties) {
        modelPathToProperties.put(modelProperties.modelPath, modelProperties);
    }

    /**
     * Add two Neural Network Properties together.
     *
     * <p>Any properties that are the same will be overwritten by the second
     *
     * @param nnProps
     * @return itself, so it can be chained and used fluently
     */
    public NeuralNetworkModelsSettings sum(NeuralNetworkModelsSettings nnProps) {
        modelPathToProperties.putAll(nnProps.modelPathToProperties);

        return this;
    }

    /**
     * Remove a model from the list of models.
     *
     * @param modelPath
     * @return True if the model was removed, false if it was not found
     */
    public boolean removeModel(Path modelPath) {
        return modelPathToProperties.remove(modelPath) != null;
    }

    /**
     * Get the model properties for a given model path.
     *
     * @param modelPath
     * @return {@link ModelProperties} object
     */
    public ModelProperties getModel(Path modelPath) {
        return modelPathToProperties.get(modelPath);
    }

    /**
     * Get all models
     *
     * @return A list of all models
     */
    @Json.Property("models")
    public ModelProperties[] getModels() {
        return modelPathToProperties.values().toArray(new ModelProperties[0]);
    }

    /**
     * Change the nickname of a {@link ModelProperties} object.
     *
     * @param modelPath
     * @param newName
     * @return True if the model was found and renamed, false if it was not found
     */
    public boolean renameModel(Path modelPath, String newName) {
        ModelProperties temp = modelPathToProperties.get(modelPath);
        if (temp != null) {
            modelPathToProperties.remove(modelPath);
            modelPathToProperties.put(
                    modelPath,
                    new ModelProperties(
                            temp.modelPath,
                            newName,
                            temp.labels,
                            temp.resolutionWidth,
                            temp.resolutionHeight,
                            temp.family,
                            temp.version));
            return true;
        }
        return false;
    }

    public boolean clear() {
        modelPathToProperties.clear();
        return true;
    }
}
