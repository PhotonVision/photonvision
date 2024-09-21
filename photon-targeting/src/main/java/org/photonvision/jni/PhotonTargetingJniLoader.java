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
import java.nio.file.Files;
import java.util.List;
import org.photonvision.common.hardware.Platform;

public class PhotonTargetingJniLoader {
    public static boolean load() throws IOException {
        // We always extract the shared object (we could hash each so, but that's a lot
        // of work)
        String arch_name = Platform.getNativeLibraryFolderName();
        var clazz = PhotonTargetingJniLoader.class;

        for (var libraryName : List.of("photontargeting", "photontargetingJNI")) {
            var nativeLibName = System.mapLibraryName(libraryName);
            var in = clazz.getResourceAsStream("/nativelibraries/" + arch_name + "/" + nativeLibName);

            if (in == null) {
                return false;
            }

            // It's important that we don't mangle the names of these files on Windows at
            // least
            var tempfolder = Files.createTempDirectory("nativeextract");
            File temp = new File(tempfolder.toAbsolutePath().toString(), nativeLibName);
            System.out.println(temp.getAbsolutePath().toString());
            FileOutputStream fos = new FileOutputStream(temp);

            int read = -1;
            byte[] buffer = new byte[1024];
            while ((read = in.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.close();
            in.close();

            System.load(temp.getAbsolutePath());

            System.out.println("Successfully loaded shared object " + temp.getName());
        }

        return true;
    }
}
