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

package org.photonvision.jni;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public abstract class PhotonJNICommon {
    public abstract boolean isLoaded();

    public abstract void setLoaded(boolean state);

    protected static Logger logger = null;

    protected static synchronized void forceLoad(
            PhotonJNICommon instance, Class<?> clazz, List<String> libraries) throws IOException {
        if (instance.isLoaded()) return;
        if (logger == null) logger = new Logger(clazz, LogGroup.General);

        for (String libraryName : libraries) {
            try {
                logger.info("Loading " + libraryName);
                // We always extract the shared object (we could hash each so, but that's a lot of work)
                var arch_name = Platform.getNativeLibraryFolderName();
                String nativeLibName = System.mapLibraryName(libraryName);
                var in = clazz.getResourceAsStream("/nativelibraries/" + arch_name + "/" + nativeLibName);

                if (in == null) {
                    logger.error("Could not find " + libraryName);
                    instance.setLoaded(false);
                    return;
                }

                // It's important that we don't mangle the names of these files on Windows at least
                var temp = Files.createTempDirectory("nativeExtract").resolve(nativeLibName);
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);

                System.load(temp.toAbsolutePath().toString());
                logger.info("Successfully loaded shared object " + temp.getFileName());

            } catch (UnsatisfiedLinkError e) {
                logger.error("Couldn't load shared object " + libraryName, e);
                e.printStackTrace();
                instance.setLoaded(false);
                return;
            }
        }
        instance.setLoaded(true);
    }

    protected static synchronized void forceLoad(
            PhotonJNICommon instance, Class<?> clazz, String libraryName) throws IOException {
        forceLoad(instance, clazz, List.of(libraryName));
    }
}
