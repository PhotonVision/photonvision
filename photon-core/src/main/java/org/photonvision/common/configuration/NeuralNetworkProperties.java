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
import com.fasterxml.jackson.annotation.JsonProperty;
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
        public String modelPath;
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
                String modelPath,
                String nickname,
                LinkedList<String> labels,
                double resolutionWidth,
                double resolutionHeight,
                Family family,
                RknnJNI.ModelVersion rknnVersion) {
            this.modelPath = modelPath;
            this.nickname = nickname;
            this.labels = labels;
            this.resolutionHeight = resolutionHeight;
            this.resolutionWidth = resolutionWidth;
            this.family = family;
            this.rknnVersion = rknnVersion;
        }
    }

    public LinkedList<RknnModelProperties> modelProperties = new LinkedList<RknnModelProperties>();

    public NeuralNetworkProperties() {}

    @JsonCreator
    public NeuralNetworkProperties(
            @JsonProperty("modelPropertiesList") LinkedList<RknnModelProperties> modelPropertiesList) {}

    public NeuralNetworkProperties(NeuralNetworkProperties NNMProperties) {
        this(NNMProperties.modelProperties);
    }

    @Override
    public String toString() {
        String toReturn = "";

        toReturn += "NeuralNetworkProperties [";

        toReturn += modelProperties.toString() + "]";

        return toReturn;
    }
}
