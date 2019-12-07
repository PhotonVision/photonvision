package com.chameleonvision.config;

import com.chameleonvision.util.ProgramDirectoryUtilities;
import com.chameleonvision.vision.camera.CameraStreamer;
import com.chameleonvision.vision.image.StaticImageCapture;
import com.chameleonvision.vision.pipeline.impl.CVPipeline2d;
import edu.wpi.cscore.CameraServerCvJNI;
import edu.wpi.cscore.CameraServerJNI;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

class StaticCaptureTest {

    private static final List<String> allowedImageExtensions = List.of(".jpg", ".jpeg", ".png");

    private static final FilenameFilter imageExtensionFilter = (dir, name) -> allowedImageExtensions.stream().anyMatch(name::endsWith);

    private static final LinkedHashMap<String, StaticImageCapture> loadedImages = new LinkedHashMap<>();

    @BeforeAll
    static void setup() {
        try {
            CameraServerJNI.forceLoad();
            CameraServerCvJNI.forceLoad();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JNI libraries!");
        }
    }

    @Test
    void ImageLoadTest() {
        Path imagesFolder = Paths.get(ProgramDirectoryUtilities.getProgramDirectory(), "testimages", "2019");
        if (Files.exists(imagesFolder)) {
            File dir = new File(imagesFolder.toString());
            File[] imageFiles = dir.listFiles(imageExtensionFilter);

            Assertions.assertNotNull(imageFiles);

            for (File imageFile : imageFiles) {
                loadedImages.put(imageFile.getName().replace(".jpg", ""), new StaticImageCapture(imageFile.toPath(), 68.5));
            }

            Assertions.assertEquals(loadedImages.size(), imageFiles.length);
        }
    }

    @Test
    void ImageProcessTest() throws InterruptedException {
        ImageLoadTest();
        CVPipeline2d testPipeline = new CVPipeline2d();
        String testImage1 = "CargoSideStraightDark36in";
        StaticImageCapture testCapture1 = loadedImages.get(testImage1);

        testPipeline.initPipeline(testCapture1);

        var streamer = new CameraStreamer(testCapture1, "CargoSideStraightDark36in");

        NetworkTableInstance.getDefault().startClient("localhost");

        while(true) {
            var result = testPipeline.runPipeline(testCapture1.getFrame().getKey());
            streamer.runStream(result.outputMat);
            Thread.sleep(20);
        }

    }
}