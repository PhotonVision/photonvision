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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Family;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Version;

public class NeuralNetworkPropertyManager {
    /*
     * The properties of the model. This is used to determine which model to load.
     * The only families currently supported are RKNN and Rubik (custom .tflite)
     */
    public record ModelProperties(
            @JsonProperty("modelPath") Path modelPath,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("labels") LinkedList<String> labels,
            @JsonProperty("resolutionWidth") int resolutionWidth,
            @JsonProperty("resolutionHeight") int resolutionHeight,
            @JsonProperty("family") Family family,
            @JsonProperty("version") Version version) {
        @JsonCreator
        public ModelProperties {
            // Record constructor is automatically annotated with @JsonCreator
        }
    }

    // The path to the model is used as the key in the map because it is unique to
    // the model, and should not change
    @JsonProperty("modelPathToProperties")
    private HashMap<Path, ModelProperties> modelPathToProperties =
            new HashMap<Path, ModelProperties>();

    /**
     * Constructor for the NeuralNetworkProperties class.
     *
     * <p>This object holds a LinkedList of {@link ModelProperties} objects
     */
    public NeuralNetworkPropertyManager() {}

    /**
     * Constructor for the NeuralNetworkProperties class.
     *
     * <p>This object holds a LinkedList of {@link ModelProperties} objects.
     *
     * @param modelPropertiesList When the class is constructed, it will hold the provided list
     */
    public NeuralNetworkPropertyManager(HashMap<Path, ModelProperties> modelPropertiesList) {}

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
    public NeuralNetworkPropertyManager sum(NeuralNetworkPropertyManager nnProps) {
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
    @JsonIgnore
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
