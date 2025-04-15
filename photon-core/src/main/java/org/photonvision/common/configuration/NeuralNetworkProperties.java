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
    /**
     * The properties of the model. This is used to determine which model to load. The only family
     * currently supported is RKNN. If we add other families, we'll have to determine if we want to
     * expand this modelProperties object, or create separate objects for each family.
     */
    public class RknnModelProperties {
        public Path modelPath;
        public String nickname;
        public LinkedList<String> labels;
        public double resolutionWidth;
        public double resolutionHeight;
        public Family family;
        public RknnJNI.ModelVersion rknnVersion;

        /**
         * Constructor for the rknnModelProperties class.
         *
         * @param modelPath
         * @param nickname
         * @param labels
         * @param resolutionHeight
         * @param resolutionWidth
         * @param family
         * @param rknnVersion
         */
        public RknnModelProperties(
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

    protected HashMap<Path, RknnModelProperties> properties =
            new HashMap<Path, RknnModelProperties>();

    public NeuralNetworkProperties() {}

    public NeuralNetworkProperties(HashMap<Path, RknnModelProperties> modelPropertiesList) {}

    public NeuralNetworkProperties(NeuralNetworkProperties NNMProperties) {
        this(NNMProperties.properties);
    }

    @Override
    public String toString() {
        String toReturn = "";

        toReturn += "NeuralNetworkProperties [";

        toReturn += properties.toString() + "]";

        return toReturn;
    }

    public void addModelProperties(RknnModelProperties modelProperties) {
        properties.put(modelProperties.modelPath, modelProperties);
    }

    /**
     * Add two Neural Network Properties together.
     *
     * <p>Any keys in the object passed in will override those from the other object
     *
     * @param nnProps
     * @return itself, so it can be chained and for adding where you want to pass into a function
     */
    public NeuralNetworkProperties sum(NeuralNetworkProperties nnProps) {
        properties.putAll(nnProps.properties);

        return this;
    }

    public boolean removeModel(Path modelPath) {
        return properties.remove(modelPath) != null;
    }

    public RknnModelProperties getModel(Path modelPath) {
        return properties.get(modelPath);
    }

    public boolean renameModel(Path modelPath, String newName) {
        RknnModelProperties modelProperties = properties.get(modelPath);
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
