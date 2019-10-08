package com.chameleonvision.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHelper {
    private FileHelper() {} // no construction, utility class

    public static void CheckPath(String path) {
        if (path.equals("")) return;
        Path realPath = Path.of(path);
        CheckPath(realPath);
    }

    public static void CheckPath(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
