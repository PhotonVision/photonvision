package com.photonvision.apple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class CoreMLTestUtils {
    private static boolean initialized = false;

    /** Initialize OpenCV library */
    public static void initializeLibraries() {
        if (initialized) return;

        // Load OpenCV native library
        try {
            System.loadLibrary("opencv_java4100");
            System.out.println("OpenCV library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load OpenCV library: " + e.getMessage());
            System.err.println("java.library.path: " + System.getProperty("java.library.path"));
            throw e;
        }

        initialized = true;
    }

    /**
     * Load test image from resources
     *
     * @param imageName Image name in test resources
     * @return OpenCV Mat object
     */
    public static Mat loadTestImage(String imageName) {
        String imagePath = getResourcePath("2025/" + imageName);
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            throw new RuntimeException("Failed to load image: " + imagePath);
        }
        return image;
    }

    /**
     * Load test model from resources
     *
     * @param modelName Model name in test resources
     * @return Path to model file
     */
    public static String loadTestModel(String modelName) {
        return getResourcePath("2025/" + modelName);
    }

    /**
     * Get absolute path for a resource file
     *
     * @param resourcePath Relative path in test resources
     * @return Absolute path to resource
     */
    private static String getResourcePath(String resourcePath) {
        return Paths.get(System.getProperty("user.dir"), "src", "test", "resources", resourcePath)
                .toString();
    }

    /**
     * Save detection result image for debugging
     *
     * @param image Image with detection results drawn
     * @param filename Output filename
     */
    public static void saveDebugImage(Mat image, String filename) {
        Path outputPath = Paths.get(System.getProperty("user.dir"), "build", "test-results", filename);
        try {
            Files.createDirectories(outputPath.getParent());
            Imgcodecs.imwrite(outputPath.toString(), image);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
