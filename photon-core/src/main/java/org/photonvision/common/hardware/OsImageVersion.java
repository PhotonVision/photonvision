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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

/**
 * Our blessed images inject the current version via this build workflow:
 * https://github.com/PhotonVision/photon-image-modifier/blob/2e5ddb6b599df0be921c12c8dbe7b939ecd7f615/.github/workflows/main.yml#L67
 *
 * <p>This class provides a convenient abstraction around this
 */
public class OsImageVersion {
    private static final Logger logger = new Logger(OsImageVersion.class, LogGroup.General);

    private static Path imageVersionFile = Path.of("/opt/photonvision/image-version");

    public static final Optional<String> IMAGE_VERSION = getImageVersion();

    private static Optional<String> getImageVersion() {
        if (!imageVersionFile.toFile().exists()) {
            logger.warn(
                    "Photon cannot locate base OS image version metadata at " + imageVersionFile.toString());
            return Optional.empty();
        }

        try {
            return Optional.of(Files.readString(imageVersionFile).strip());
        } catch (IOException e) {
            logger.error("Couldn't read image-version file", e);
        }

        return Optional.empty();
    }
}
