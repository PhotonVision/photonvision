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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import org.photonvision.common.configuration.NeuralNetworkModelManager.Family;
import org.photonvision.rknn.RknnJNI;

public class NeuralNetworkProperties {
    /*
     * The properties of the model. This is used to determine which model to load.
     * The only family
     * currently supported is RKNN. If we add other families, we'll have to
     * determine if we want to
     * expand this modelProperties object, or create separate objects for each
     * family. A suggested solution is to add other version attributes, and the
     * attribute for an unused version is to be left null.
     */
    public class ModelProperties {
        public Path modelPath;
        public String nickname;
        public LinkedList<String> labels;
        public double resolutionWidth;
        public double resolutionHeight;
        public Family family;
        public RknnJNI.ModelVersion rknnVersion;

        /**
         * This object holds the various properties for an object detection model
         *
         * @param modelPath path to the model on disk
         * @param nickname name to use in the UI
         * @param labels labels for the models outputs
         * @param resolutionHeight
         * @param resolutionWidth
         * @param family the family of the model [RKNN]
         * @param rknnVersion the version of the RKNN model [YOLO_V5, YOLO_V8, YOLO_V11]
         */
        public ModelProperties(
                Path modelPath,
                String nickname,
                LinkedList<String> labels,
                double resolutionWidth,
                double resolutionHeight,
                Family family,
                RknnJNI.ModelVersion rknnVersion) {
            this.modelPath = modelPath;
            this.nickname = nickname;
            this.labels = labels;
            this.resolutionWidth = resolutionWidth;
            this.resolutionHeight = resolutionHeight;
            this.family = family;
            this.rknnVersion = rknnVersion;
        }
    }

    // The path to the model is used as the key in the map because it is unique to
    // the model, and should not change
    protected HashMap<Path, ModelProperties> properties = new HashMap<Path, ModelProperties>();

    /**
     * Constructor for the NeuralNetworkProperties class.
     *
     * <p>This object holds a LinkedList of {@link ModelProperties} objects
     */
    public NeuralNetworkProperties() {}

    /**
     * Constructor for the NeuralNetworkProperties class.
     *
     * <p>This object holds a LinkedList of {@link ModelProperties} objects.
     *
     * @param modelPropertiesList When the class is constructed, it will hold the provided list
     */
    public NeuralNetworkProperties(HashMap<Path, ModelProperties> modelPropertiesList) {}

    @Override
    public String toString() {
        String toReturn = "";

        toReturn += "NeuralNetworkProperties [";

        toReturn += properties.toString() + "]";

        return toReturn;
    }

    /**
     * Add a model to the list of models.
     *
     * @param modelProperties
     */
    public void addModelProperties(ModelProperties modelProperties) {
        properties.put(modelProperties.modelPath, modelProperties);
    }

    /**
     * Add two Neural Network Properties together.
     *
     * <p>Any properties that are the same will be overwritten by the second
     *
     * @param nnProps
     * @return itself, so it can be chained and used fluently
     */
    public NeuralNetworkProperties sum(NeuralNetworkProperties nnProps) {
        properties.putAll(nnProps.properties);

        return this;
    }

    /**
     * Remove a model from the list of models.
     *
     * @param modelPath
     * @return True if the model was removed, false if it was not found
     */
    public boolean removeModel(Path modelPath) {
        return properties.remove(modelPath) != null;
    }

    /**
     * Get the model properties for a given model path.
     *
     * @param modelPath
     * @return {@link ModelProperties} object
     */
    public ModelProperties getModel(Path modelPath) {
        return properties.get(modelPath);
    }

    /**
     * Change the nickname of a {@link ModelProperties} object.
     *
     * @param modelPath
     * @param newName
     * @return True if the model was found and renamed, false if it was not found
     */
    public boolean renameModel(Path modelPath, String newName) {
        ModelProperties modelProperties = properties.get(modelPath);
        if (modelProperties != null) {
            modelProperties.nickname = newName;
            return true;
        }
        return false;
    }

    public void export() {
        // TODO: this should return a json file or smth of all the various properties
    }
}
