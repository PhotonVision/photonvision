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

package org.photonvision.common.hardware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/**
 * Our blessed images inject the current version via the build process in
 * https://github.com/PhotonVision/photon-image-modifier
 *
 * <p>This class provides a convenient abstraction around this
 */
public class OsImageData {
    private static final Logger logger = new Logger(OsImageData.class, LogGroup.General);

    private static Path imageVersionFile = Path.of("/opt/photonvision/image-version");
    private static Path imageMetadataFile = Path.of("/opt/photonvision/image-version.json");

    /** The OS image version string, if available. This is legacy, use {@link ImageMetadata}. */
    public static final Optional<String> IMAGE_VERSION = getImageVersion();

    private static Optional<String> getImageVersion() {
        if (!imageVersionFile.toFile().exists()) {
            logger.warn("Photon cannot locate base OS image version at " + imageVersionFile.toString());
            return Optional.empty();
        }

        try {
            return Optional.of(Files.readString(imageVersionFile).strip());
        } catch (IOException e) {
            logger.error("Couldn't read image-version file", e);
        }

        return Optional.empty();
    }

    public static final Optional<ImageMetadata> IMAGE_METADATA = getImageMetadata();

    public static record ImageMetadata(
            String buildDate, String commitSha, String commitTag, String imageName, String imageSource) {}

    private static Optional<ImageMetadata> getImageMetadata() {
        if (!imageMetadataFile.toFile().exists()) {
            logger.warn("Photon cannot locate OS image metadata at " + imageMetadataFile.toString());
            return Optional.empty();
        }

        try {
            String content = Files.readString(imageMetadataFile).strip();

            ObjectMapper mapper =
                    new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            ImageMetadata md = mapper.readValue(content, ImageMetadata.class);

            if (md.buildDate() == null
                    && md.commitSha() == null
                    && md.commitTag() == null
                    && md.imageName() == null
                    && md.imageSource() == null) {
                logger.warn(
                        "OS image metadata JSON did not contain recognized fields; preserving legacy behavior");
                return Optional.empty();
            }

            return Optional.of(md);
        } catch (IOException e) {
            logger.error("Couldn't read image metadata file", e);
        } catch (Exception e) {
            logger.error("Failed to parse image metadata", e);
        }

        return Optional.empty();
    }
}
