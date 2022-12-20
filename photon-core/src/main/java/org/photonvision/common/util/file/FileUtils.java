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

package org.photonvision.common.util.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class FileUtils {
    private FileUtils() {}

    private static final Logger logger = new Logger(FileUtils.class, LogGroup.General);
    private static final Set<PosixFilePermission> allReadWriteExecutePerms =
            new HashSet<>(Arrays.asList(PosixFilePermission.values()));

    public static void deleteDirectory(Path path) {
        try {
            // create a stream
            var files = Files.walk(path);

            // delete directory including files and sub-folders
            files
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .forEach((var file) -> deleteFile(file.toPath()));

            // close the stream
            files.close();
        } catch (IOException e) {
            logger.error("Exception deleting files in " + path + "!", e);
        }
    }

    public static void deleteFile(Path path) {
        try {
            Files.delete(path);
        } catch (FileNotFoundException | NoSuchFileException fe) {
            logger.warn("Tried to delete file \"" + path + "\" but it did not exist");
        } catch (IOException e) {
            logger.error("Exception deleting file \"" + path + "\"!", e);
        }
    }

    public static void copyFile(Path src, Path dst) {
        try {
            Files.copy(src, dst);
        } catch (IOException e) {
            logger.error("Exception copying file " + src + " to " + dst + "!", e);
        }
    }

    public static void setFilePerms(Path path) throws IOException {
        if (Platform.isLinux()) {
            File thisFile = path.toFile();
            Set<PosixFilePermission> perms =
                    Files.readAttributes(path, PosixFileAttributes.class).permissions();
            if (!perms.equals(allReadWriteExecutePerms)) {
                logger.info("Setting perms on" + path);
                Files.setPosixFilePermissions(path, perms);
                var theseFiles = thisFile.listFiles();
                if (thisFile.isDirectory() && theseFiles != null) {
                    for (File subfile : theseFiles) {
                        setFilePerms(subfile.toPath());
                    }
                }
            }
        }
    }

    public static void setAllPerms(Path path) {
        if (Platform.isLinux()) {
            String command = String.format("chmod 777 -R %s", path.toString());
            try {
                Process p = Runtime.getRuntime().exec(command);
                p.waitFor();

            } catch (Exception e) {
                logger.error("Setting perms failed!", e);
            }
        } else {
            logger.info("Cannot set directory permissions on Windows!");
        }
    }
}
