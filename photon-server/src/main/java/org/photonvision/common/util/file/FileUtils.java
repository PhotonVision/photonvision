package org.photonvision.common.util.file;

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FileUtils {

    private FileUtils() {}

    private static Logger logger = new Logger(FileUtils.class, LogGroup.General);
    private static final Set<PosixFilePermission> allReadWriteExecutePerms =
            new HashSet<>(Arrays.asList(PosixFilePermission.values()));

    public static void setFilePerms(Path path) throws IOException {
        if (!Platform.CurrentPlatform.isWindows()) {
            File thisFile = path.toFile();
            Set<PosixFilePermission> perms =
                    Files.readAttributes(path, PosixFileAttributes.class).permissions();
            if (!perms.equals(allReadWriteExecutePerms)) {
                logger.info("Setting perms on" + path.toString());
                Files.setPosixFilePermissions(path, perms);
                if (thisFile.isDirectory()) {
                    for (File subfile : thisFile.listFiles()) {
                        setFilePerms(subfile.toPath());
                    }
                }
            }
        }
    }

    public static void setAllPerms(Path path) {
        if (!Platform.CurrentPlatform.isWindows()) {
            String command = String.format("chmod 777 -R %s", path.toString());
            try {
                Process p = Runtime.getRuntime().exec(command);
                p.waitFor();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            logger.info("Cannot set directory permissions on Windows!");
        }
    }
}
