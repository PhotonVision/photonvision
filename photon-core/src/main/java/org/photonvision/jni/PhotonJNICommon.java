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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public abstract class PhotonJNICommon {
    protected static Logger logger = null;

    protected static Set<String> loadedLibraries = new HashSet<>();

    protected static synchronized void forceLoad(Class<?> clazz, List<String> libraries) {
        if (logger == null) logger = new Logger(clazz, LogGroup.Camera);

        for (var libraryName : libraries) {
            if (loadedLibraries.contains(clazz.getName() + ":" + libraryName)) {
                logger.info("Library " + libraryName + " already loaded");
                continue;
            }
            try {
                // We always extract the shared object (we could hash each so, but that's a lot of work)
                var lib = unpack(clazz, libraryName);
                System.load(lib);
                loadedLibraries.add(clazz.getName() + ":" + libraryName);
            } catch (Exception e) {
                logger.error("Couldn't load shared object " + libraryName, e);
                e.printStackTrace();
                break;
            }
        }
    }

    protected static synchronized void forceLoad(Class<?> clazz, String libraryName) {
        forceLoad(clazz, List.of(libraryName));
    }

    protected static synchronized String unpack(Class<?> clazz, String libraryName, String unpackTo) {
        System.out.println("Unpacking library " + libraryName);
        var arch_name = Platform.getNativeLibraryFolderName();
        var nativeLibName = System.mapLibraryName(libraryName);
        var in = clazz.getResourceAsStream("/nativelibraries/" + arch_name + "/" + nativeLibName);

        if (in == null) {
            System.out.println("Couldn't find library " + arch_name + "/" + nativeLibName);
            return null;
        }
        String res = null;
        try {
            // It's important that we don't mangle the names of these files on Windows at least
            File temp = new File(unpackTo, nativeLibName);
            if (temp.exists()) temp.delete();
            FileOutputStream fos = new FileOutputStream(temp);

            int read = -1;
            byte[] buffer = new byte[1024];
            while ((read = in.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.close();
            in.close();
            res = temp.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Unpacked library " + libraryName + " to " + res);
        return res;
    }

    protected static synchronized String unpack(Class<?> clazz, String libraryName) {
        return unpack(clazz, libraryName, System.getProperty("java.io.tmpdir"));
    }

    public static boolean isWorking(Class<? extends PhotonJNICommon> clazz) {
        boolean working = false;
        for (var lib : loadedLibraries) {
            if (lib.contains(clazz.getName())) {
                working = true;
                break;
            }
        }
        return working;
    }
}
