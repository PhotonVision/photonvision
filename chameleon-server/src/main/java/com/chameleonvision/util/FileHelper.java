package com.chameleonvision.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FileHelper {
    private FileHelper() {}

    private static final Set<PosixFilePermission> allReadWriteExecutePerms = new HashSet<>(Arrays.asList(PosixFilePermission.values()));

    public static void setFilePerms(Path path) throws IOException {
        if (!Platform.CurrentPlatform.isWindows()) {
            Set<PosixFilePermission> perms = Files.readAttributes(path, PosixFileAttributes.class).permissions();
            if (!perms.equals(allReadWriteExecutePerms)) {
                Files.setPosixFilePermissions(path, perms);
            }
        }
    }
}
