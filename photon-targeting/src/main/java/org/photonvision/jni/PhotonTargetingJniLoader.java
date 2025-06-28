/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.photonvision.jni;

import edu.wpi.first.util.RuntimeLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.photonvision.common.hardware.Platform;

public class PhotonTargetingJniLoader {
    public static boolean isWorking = false;

    public static boolean load() throws IOException, UnsatisfiedLinkError {
        if (isWorking) return true;
        isWorking = load_();
        return isWorking;
    }

    public static boolean load_() throws IOException, UnsatisfiedLinkError {
        // We always extract the shared object (we could hash each so, but that's a lot
        // of work)
        String arch_name = Platform.getNativeLibraryFolderName();
        var clazz = PhotonTargetingJniLoader.class;

        for (var libraryName : List.of("photontargeting", "photontargetingJNI")) {
            try {
                RuntimeLoader.loadLibrary(libraryName);
                continue;
            } catch (Exception e) {
                System.out.println("Direct library load failed; falling back to extraction");
            }

            var nativeLibName = System.mapLibraryName(libraryName);
            var path = "/nativelibraries/" + arch_name + "/" + nativeLibName;
            var in = clazz.getResourceAsStream(path);

            if (in == null) {
                System.err.println("Could not get resource at path " + path);
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

            try {
                System.load(temp.getAbsolutePath());
            } catch (Throwable t) {
                System.err.println("Unable to System.load " + temp.getName() + " : " + t.getMessage());
                t.printStackTrace();
                return false;
            }

            System.out.println("Successfully loaded shared object " + temp.getName());
        }

        return true;
    }
}
