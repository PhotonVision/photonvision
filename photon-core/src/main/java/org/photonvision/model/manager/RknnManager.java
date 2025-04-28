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

package org.photonvision.model.manager;

import io.javalin.http.UploadedFile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.model.vision.Model;
import org.photonvision.model.vision.RknnModel;
import org.photonvision.rknn.RknnJNI;

public class RknnManager implements ModelManager {
    private static final Logger logger = new Logger(RknnManager.class, LogGroup.Config);
    private static final String BACKEND_NAME = "RKNN";
    private static final String PRIMARY_EXTENSION = ".rknn";
    private static final String UPLOAD_EXTENSION = ".rknn";
    private static final Class<? extends Model> MODEL_CLASS = RknnModel.class;

    // Naming convention patterns
    private static final Pattern modelPattern =
            Pattern.compile("^([a-zA-Z0-9._-]+)-(\\d+)-(\\d+)-(yolov(?:5|8|11)[nsmlx]*)\\.rknn$");
    private static final Pattern labelsPattern =
            Pattern.compile("^([a-zA-Z0-9._-]+)-(\\d+)-(\\d+)-(yolov(?:5|8|11)[nsmlx]*)-labels\\.txt$");

    @Override
    public String getBackendName() {
        return BACKEND_NAME;
    }

    @Override
    public String getUploadAcceptType() {
        return UPLOAD_EXTENSION;
    }

    @Override
    public Class<? extends Model> getModelClass() {
        return MODEL_CLASS;
    }

    @Override
    public Info getInfo() {
        return new Info(getBackendName(), getUploadAcceptType());
    }

    @Override
    public boolean supportsPath(Path path) {
        if (path == null) return false;
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(PRIMARY_EXTENSION);
    }

    @Override
    public boolean supportsUpload(String modelFileName, String labelsFileName) {
        if (modelFileName == null || labelsFileName == null) return false;
        try {
            verifyNames(modelFileName, labelsFileName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public Optional<String> validateUpload(UploadedFile modelFile, UploadedFile labelsFile) {
        String modelExtension = getExtension(modelFile.filename()).toLowerCase();
        String labelsExtension = getExtension(labelsFile.filename()).toLowerCase();

        if (!modelExtension.equals(UPLOAD_EXTENSION)) {
            return Optional.of(
                    "Invalid model file type. Expected '"
                            + UPLOAD_EXTENSION
                            + "' but got '"
                            + modelExtension
                            + "'");
        }
        if (!labelsExtension.equals(".txt")) {
            return Optional.of(
                    "Invalid labels file type. Expected '.txt' but got '" + labelsExtension + "'");
        }

        try {
            verifyNames(modelFile.filename(), labelsFile.filename());
        } catch (IllegalArgumentException e) {
            return Optional.of("Invalid file naming convention: " + e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public void verifyNames(String modelFileName, String labelsFileName)
            throws IllegalArgumentException {
        if (modelFileName == null || labelsFileName == null) {
            throw new IllegalArgumentException("Model name and labels name cannot be null");
        }

        Matcher modelMatcher = modelPattern.matcher(modelFileName);
        Matcher labelsMatcher = labelsPattern.matcher(labelsFileName);

        logger.debug("Verifying RKNN names - Model: " + modelFileName + ", Labels: " + labelsFileName);

        if (!modelMatcher.matches()) {
            throw new IllegalArgumentException(
                    "Model name '"
                            + modelFileName
                            + "' must follow the convention name-width-height-version"
                            + PRIMARY_EXTENSION);
        }
        if (!labelsMatcher.matches()) {
            throw new IllegalArgumentException(
                    "Labels name '"
                            + labelsFileName
                            + "' must follow the convention name-width-height-version-labels.txt");
        }

        // Check if all captured groups match
        if (!modelMatcher.group(1).equals(labelsMatcher.group(1)) // baseName
                || !modelMatcher.group(2).equals(labelsMatcher.group(2)) // width
                || !modelMatcher.group(3).equals(labelsMatcher.group(3)) // height
                || !modelMatcher.group(4).equals(labelsMatcher.group(4))) { // versionString
            throw new IllegalArgumentException(
                    "Model name ('"
                            + modelFileName
                            + ") and labels name ('"
                            + labelsFileName
                            + ") parts must match.");
        }

        // Additionally parse and check numeric parts and version
        try {
            parseModelName(modelFileName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid components in model name: " + e.getMessage(), e);
        }
    }

    @Override
    public Model loadFromPath(Path path, File modelsDirectory)
            throws IOException, IllegalArgumentException {
        String modelFileName = path.getFileName().toString();
        logger.info("Loading RKNN model from path: " + path);

        // 1. Parse model name to get info (implicitly validates format)
        ParsedModelInfo parsedInfo;
        try {
            parsedInfo = parseModelName(modelFileName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to parse model filename: " + modelFileName, e);
        }

        // 2. Determine and find the labels file
        String labelsFileName = deriveLabelsName(parsedInfo);
        Path labelsPath = modelsDirectory.toPath().resolve(labelsFileName);

        if (!Files.exists(labelsPath)) {
            throw new IOException("Could not find expected labels file: " + labelsPath);
        }
        if (!Files.isRegularFile(labelsPath)) {
            throw new IOException("Expected labels file is not a regular file: " + labelsPath);
        }

        // 3. Read labels
        List<String> labels;
        try {
            labels = Files.readAllLines(labelsPath);
            logger.debug("Successfully read labels from: " + labelsPath);
        } catch (IOException e) {
            throw new IOException("Failed to read labels file: " + labelsPath, e);
        }

        // 4. Create the RknnModel instance
        try {
            return new RknnModel(
                    path.toFile(),
                    labels,
                    parseVersionString(parsedInfo.versionString),
                    parsedInfo.inputSize);
        } catch (Exception e) {
            throw new IOException("Failed to instantiate RknnModel for " + modelFileName, e);
        }
    }

    @Override
    public void saveUploadedFiles(
            UploadedFile modelFile, UploadedFile labelsFile, File modelsDirectory) throws IOException {
        if (!modelsDirectory.exists()) {
            if (!modelsDirectory.mkdirs()) {
                throw new IOException(
                        "Failed to create models directory: " + modelsDirectory.getAbsolutePath());
            }
        }
        if (!modelsDirectory.isDirectory()) {
            throw new IOException(
                    "Models directory path is not a directory: " + modelsDirectory.getAbsolutePath());
        }

        try {
            verifyNames(modelFile.filename(), labelsFile.filename());
        } catch (IllegalArgumentException e) {
            throw new IOException("Uploaded file names are invalid: " + e.getMessage(), e);
        }

        Path labelsDestPath = modelsDirectory.toPath().resolve(labelsFile.filename());
        Path modelDestPath = modelsDirectory.toPath().resolve(modelFile.filename());

        logger.info("Saving RKNN files to: " + modelsDirectory.getAbsolutePath());
        logger.debug("Saving labels to: " + labelsDestPath);
        logger.debug("Saving model to: " + modelDestPath);

        try (InputStream in = labelsFile.content();
                OutputStream out = Files.newOutputStream(labelsDestPath)) {
            in.transferTo(out);
        } catch (IOException e) {
            throw new IOException("Failed to save RKNN labels file: " + labelsDestPath, e);
        }

        try (InputStream in = modelFile.content();
                OutputStream out = Files.newOutputStream(modelDestPath)) {
            in.transferTo(out);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(labelsDestPath);
                logger.warn("Deleted labels file " + labelsDestPath + " due to model save failure.");
            } catch (IOException ignored) {
                logger.error(
                        "Failed to delete labels file " + labelsDestPath + " after model save failure.",
                        ignored);
            }
            throw new IOException("Failed to save RKNN model file: " + modelDestPath, e);
        }
        logger.info(
                "Successfully saved RKNN model "
                        + modelFile.filename()
                        + " and labels "
                        + labelsFile.filename());
    }

    /** Parse RKNN model file name and return parsed info. */
    public ParsedModelInfo parseModelName(String modelFileName) throws IllegalArgumentException {
        Matcher modelMatcher = modelPattern.matcher(modelFileName);

        if (!modelMatcher.matches()) {
            throw new IllegalArgumentException(
                    "Model name '"
                            + modelFileName
                            + "' must follow the convention name-width-height-version"
                            + PRIMARY_EXTENSION);
        }

        try {
            String baseName = modelMatcher.group(1);
            int width = Integer.parseInt(modelMatcher.group(2));
            int height = Integer.parseInt(modelMatcher.group(3));
            String versionString = modelMatcher.group(4);

            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Width and height must be positive integers.");
            }

            parseVersionString(versionString);

            return new ParsedModelInfo(baseName, width, height, versionString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid width/height number in model name: " + modelFileName, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid version string in model name: " + modelFileName + " -> " + e.getMessage(), e);
        }
    }

    /** Determines the model version enum based on the version string from the filename. */
    private RknnJNI.ModelVersion parseVersionString(String versionString)
            throws IllegalArgumentException {
        if (versionString.startsWith("yolov5")) {
            return RknnJNI.ModelVersion.YOLO_V5;
        } else if (versionString.startsWith("yolov8")) {
            return RknnJNI.ModelVersion.YOLO_V8;
        } else if (versionString.startsWith("yolov11")) {
            return RknnJNI.ModelVersion.YOLO_V11;
        } else {
            throw new IllegalArgumentException("Unknown model version string: " + versionString);
        }
    }

    /** Derive the labels filename from parsed model info. */
    private String deriveLabelsName(ParsedModelInfo parsedInfo) {
        return parsedInfo.baseName
                + "-"
                + parsedInfo.width
                + "-"
                + parsedInfo.height
                + "-"
                + parsedInfo.versionString
                + "-labels.txt";
    }

    /** Helper to get file extension */
    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot);
    }
}
