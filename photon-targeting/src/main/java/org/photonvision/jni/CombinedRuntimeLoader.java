/*
 * Copyright (c) FIRST and other WPILib contributors.
 * Open Source Software; you can modify and/or share it under the terms of
 * the WPILib BSD license below:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of FIRST, WPILib, nor the names of other WPILib
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 */

package org.photonvision.jni;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Loads dynamic libraries for all platforms. */
public final class CombinedRuntimeLoader {
    private CombinedRuntimeLoader() {}

    private static String extractionDirectory;

    private static final Object extractCompleteLock = new Object();
    private static boolean extractAndVerifyComplete = false;
    private static List<String> extractedFiles = new CopyOnWriteArrayList<>();

    /**
     * Returns library extraction directory.
     *
     * @return Library extraction directory.
     */
    public static synchronized String getExtractionDirectory() {
        return extractionDirectory;
    }

    private static synchronized void setExtractionDirectory(String directory) {
        extractionDirectory = directory;
    }

    private static String defaultExtractionRoot;

    /**
     * Gets the default extraction root location (~/.wpilib/nativecache) for use if
     * setExtractionDirectory is not set.
     *
     * @return The default extraction root location.
     */
    public static synchronized String getDefaultExtractionRoot() {
        if (defaultExtractionRoot != null) {
            return defaultExtractionRoot;
        }
        String home = System.getProperty("user.home");
        defaultExtractionRoot = Paths.get(home, ".wpilib", "nativecache").toString();
        return defaultExtractionRoot;
    }

    /**
     * Returns platform path.
     *
     * @return The current platform path.
     * @throws IllegalStateException Thrown if the operating system is unknown.
     */
    public static String getPlatformPath() {
        String filePath;
        String arch = System.getProperty("os.arch");

        boolean intel32 = "x86".equals(arch) || "i386".equals(arch);
        boolean intel64 = "amd64".equals(arch) || "x86_64".equals(arch);

        if (System.getProperty("os.name").startsWith("Windows")) {
            if (intel32) {
                filePath = "/windows/x86/";
            } else {
                filePath = "/windows/x86-64/";
            }
        } else if (System.getProperty("os.name").startsWith("Mac")) {
            filePath = "/osx/universal/";
        } else if (System.getProperty("os.name").startsWith("Linux")) {
            if (intel32) {
                filePath = "/linux/x86/";
            } else if (intel64) {
                filePath = "/linux/x86-64/";
            } else if (new File("/usr/local/frc/bin/frcRunRobot.sh").exists()) {
                filePath = "/linux/athena/";
            } else if ("arm".equals(arch) || "arm32".equals(arch)) {
                filePath = "/linux/arm32/";
            } else if ("aarch64".equals(arch) || "arm64".equals(arch)) {
                filePath = "/linux/arm64/";
            } else {
                filePath = "/linux/nativearm/";
            }
        } else {
            throw new IllegalStateException();
        }

        return filePath;
    }

    private static String getLoadErrorMessage(String libraryName, UnsatisfiedLinkError ule) {
        StringBuilder msg = new StringBuilder(512);
        msg.append(libraryName)
                .append(" could not be loaded from path\n" + "\tattempted to load for platform ")
                .append(getPlatformPath())
                .append("\nLast Load Error: \n")
                .append(ule.getMessage())
                .append('\n');
        if (System.getProperty("os.name").startsWith("Windows")) {
            msg.append(
                    "A common cause of this error is missing the C++ runtime.\n"
                            + "Download the latest at https://support.microsoft.com/en-us/help/2977003/the-latest-supported-visual-c-downloads\n");
        }
        return msg.toString();
    }

    /**
     * Architecture-specific information containing file hashes for a specific CPU architecture (e.g.,
     * x86-64, arm64).
     */
    public record ArchInfo(Map<String, String> fileHashes) {}

    /**
     * Platform-specific information containing architectures for a specific OS platform (e.g., linux,
     * windows).
     */
    public record PlatformInfo(Map<String, ArchInfo> architectures) {}

    /** Overall resource information to be serialized */
    public record ResourceInformation(
            // Combined MD5 hash of all native resource files
            String hash,
            // Platform-specific native libraries organized by platform then architecture
            Map<String, PlatformInfo> platforms,
            // List of supported versions for these native resources
            List<String> versions) {}

    /**
     * Extract a list of native libraries.
     *
     * @param <T> The class where the resources would be located
     * @param clazz The actual class object
     * @param resourceName The resource name on the classpath to use for file lookup
     * @return List of all libraries that were extracted
     * @throws IOException Thrown if resource not found or file could not be extracted
     */
    public static <T> List<String> extractLibraries(Class<T> clazz, String resourceName)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ResourceInformation resourceInfo;
        try (var stream = clazz.getResourceAsStream(resourceName)) {
            resourceInfo = mapper.readValue(stream, ResourceInformation.class);
        }

        var platformPath = Paths.get(getPlatformPath());
        var platform = platformPath.getName(0).toString();
        var arch = platformPath.getName(1).toString();

        var platformInfo = resourceInfo.platforms().get(platform);
        if (platformInfo == null) {
            throw new IOException("Platform " + platform + " not found in resource information");
        }

        var archInfo = platformInfo.architectures().get(arch);
        if (archInfo == null) {
            throw new IOException("Architecture " + arch + " not found for platform " + platform);
        }

        // Map of <file to extract> to <hash we loaded from the JSON>
        Map<String, String> filenameToHash = archInfo.fileHashes();

        var extractionPathString = getExtractionDirectory();

        if (extractionPathString == null) {
            // Folder to extract to derived from overall hash
            String combinedHash = resourceInfo.hash();

            var defaultExtractionRoot = getDefaultExtractionRoot();
            var extractionPath = Paths.get(defaultExtractionRoot, platform, arch, combinedHash);
            extractionPathString = extractionPath.toString();

            setExtractionDirectory(extractionPathString);
        }

        List<String> extractedFiles = new ArrayList<>();

        for (String file : filenameToHash.keySet()) {
            try (var stream = clazz.getResourceAsStream(file)) {
                Objects.requireNonNull(stream);

                var outputFile = Paths.get(extractionPathString, new File(file).getName());

                String fileHash = filenameToHash.get(file);

                extractedFiles.add(outputFile.toString());
                if (outputFile.toFile().exists()) {
                    if (hashEm(outputFile.toFile()).equals(fileHash)) {
                        continue;
                    } else {
                        // Hashes don't match, delete and re-extract
                        System.err.println(
                                outputFile.toAbsolutePath().toString() + " failed validation - deleting");
                        outputFile.toFile().delete();
                    }
                }
                var parent = outputFile.getParent();
                if (parent == null) {
                    throw new IOException("Output file has no parent");
                }
                parent.toFile().mkdirs();

                try (var os = Files.newOutputStream(outputFile)) {
                    Files.copy(stream, outputFile, StandardCopyOption.REPLACE_EXISTING);
                }

                if (!hashEm(outputFile.toFile()).equals(fileHash)) {
                    throw new IOException("Hash of extracted file does not match expected hash");
                }
            }
        }

        return extractedFiles;
    }

    private static String hashEm(File f) throws IOException {
        try {
            MessageDigest fileHash = MessageDigest.getInstance("MD5");
            try (var dis =
                    new DigestInputStream(new BufferedInputStream(new FileInputStream(f)), fileHash)) {
                dis.readAllBytes();
            }
            var ret = HexFormat.of().formatHex(fileHash.digest());
            return ret;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Unable to verify extracted native files", e);
        }
    }

    /**
     * Load a single library from a list of extracted files.
     *
     * @param libraryName The library name to load
     * @param extractedFiles The extracted files to search
     * @throws IOException If library was not found
     */
    public static void loadLibrary(String libraryName, List<String> extractedFiles)
            throws IOException {
        String currentPath = null;
        try {
            for (var extractedFile : extractedFiles) {
                if (extractedFile.contains(libraryName)) {
                    // Load it
                    currentPath = extractedFile;
                    System.load(extractedFile);
                    return;
                }
            }
            throw new IOException("Could not find library " + libraryName);
        } catch (UnsatisfiedLinkError ule) {
            throw new IOException(getLoadErrorMessage(currentPath, ule));
        }
    }

    /**
     * Load a list of native libraries out of a single directory.
     *
     * @param <T> The class where the resources would be located
     * @param clazz The actual class object
     * @param librariesToLoad List of libraries to load
     * @throws IOException Throws an IOException if not found
     */
    public static <T> void loadLibraries(Class<T> clazz, String... librariesToLoad)
            throws IOException {
        synchronized (extractCompleteLock) {
            if (extractAndVerifyComplete == false) {
                // Extract everything
                extractedFiles = extractLibraries(clazz, "/ResourceInformation.json");
                extractAndVerifyComplete = true;
            }

            for (var library : librariesToLoad) {
                loadLibrary(library, extractedFiles);
            }
        }
    }
}
