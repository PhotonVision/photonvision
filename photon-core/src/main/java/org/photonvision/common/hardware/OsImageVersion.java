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
 * This class provides a convienent abstraction around this
 */
public class OsImageVersion {
    private static final Logger logger = new Logger(OsImageVersion.class, LogGroup.General);

    private static Path imageVersionFile = Path.of("/opt/photonvision/image-version");

    public static final Optional<String> IMAGE_VERSION = getImageVersion();
    
    private static Optional<String> getImageVersion() {
        if (!imageVersionFile.toFile().exists()) {
            logger.warn("Photon cannot locate base OS image version metadata at " + imageVersionFile.toString());
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
