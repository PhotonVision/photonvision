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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
        if (logger == null) logger = new Logger(clazz, LogGroup.Camera);

        for (var libraryName : libraries) {
            try {
                // We always extract the shared object (we could hash each so, but that's a lot of work)
                var arch_name = Platform.getNativeLibraryFolderName();
                var nativeLibName = System.mapLibraryName(libraryName);
                var in = clazz.getResourceAsStream("/nativelibraries/" + arch_name + "/" + nativeLibName);

                if (in == null) {
                    instance.setLoaded(false);
                    return;
                }

                // It's important that we don't mangle the names of these files on Windows at least
                File temp = new File(System.getProperty("java.io.tmpdir"), nativeLibName);
                FileOutputStream fos = new FileOutputStream(temp);

                int read = -1;
                byte[] buffer = new byte[1024];
                while ((read = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.close();
                in.close();

                System.load(temp.getAbsolutePath());

                logger.info("Successfully loaded shared object " + temp.getName());

            } catch (UnsatisfiedLinkError e) {
                logger.error("Couldn't load shared object " + libraryName, e);
                e.printStackTrace();
                // logger.error(System.getProperty("java.library.path"));
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
