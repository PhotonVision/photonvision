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
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public abstract class PhotonJniCommon {
    static boolean libraryLoaded = false;
    protected static Logger logger = null;

    /**
     * Load the native library. By default, we provide PhotonJniCommon as the logger name to use, but
     * subclasses should override this to provide their Class name.
     *
     * @throws IOException
     */
    public static synchronized void forceLoad() throws IOException {
        forceLoad(PhotonJniCommon.class);
    }

    protected static synchronized void forceLoad(Class<?> clazz) throws IOException {
        if (libraryLoaded) return;
        if (logger == null) logger = new Logger(clazz, LogGroup.Camera);

        try {

            // We always extract the shared object (we could hash each so, but that's a lot of work)
            var arch_name = Platform.getNativeLibraryFolderName();
            var nativeLibName = System.mapLibraryName("mrgingham");
            var in = clazz.getResourceAsStream("/nativelibraries/" + arch_name + "/" + nativeLibName);
            
            if (in == null) {
                libraryLoaded = false;
                return;
            }

            File temp = File.createTempFile(nativeLibName, "");
            FileOutputStream fos = new FileOutputStream(temp);

            int read = -1;
            byte[] buffer = new byte[1024];
            while((read = in.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.close();
            in.close();

            System.load(temp.getAbsolutePath());

            libraryLoaded = true;
            logger.info("Successfully loaded shared object");

            
        } catch (UnsatisfiedLinkError e) {
            logger.error("Couldn't load shared object", e);
            e.printStackTrace();
            logger.error(System.getProperty("java.library.path"));
        }
    }

    public static boolean isWorking() {
        return libraryLoaded;
    }
}
