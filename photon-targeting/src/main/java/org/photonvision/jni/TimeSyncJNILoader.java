package org.photonvision.jni;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class TimeSyncJNILoader {

    public static void load() throws IOException {
        // We always extract the shared object (we could hash each so, but that's a lot
        // of work)
        String arch_name = "winx64";
        var clazz = TimeSyncJNILoader.class;

        for (var libraryName : List.of("photontargeting", "photontargetingJNI")) {

            var nativeLibName = System.mapLibraryName(libraryName);
            var in = clazz.getResourceAsStream("/nativelibraries/" + arch_name + "/" + nativeLibName);

            if (in == null) {
                return;
            }

            // It's important that we don't mangle the names of these files on Windows at
            // least
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

            System.out.println("Successfully loaded shared object " + temp.getName());
        }
    }
}
