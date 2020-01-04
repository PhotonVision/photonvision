package com.chameleonvision.vision.pipeline;

import com.chameleonvision.vision.image.StaticImageCapture;
import com.chameleonvision.vision.pipeline.impl.StandardCVPipeline;
import com.chameleonvision.vision.pipeline.impl.StandardCVPipelineSettings;
import edu.wpi.cscore.CameraServerCvJNI;
import edu.wpi.cscore.CameraServerJNI;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

public class SolvePNPtest {

    private static final Path root = Path.of("src", "test", "java", "com", "chameleonvision", "vision", "pipeline");

    @Test public void test20in() {

        try {
            forceLoad();
        } catch (IOException e) {
            return;
        }

        // mock up pipeline
        var pipeline = new StandardCVPipeline();
        var capture = new StaticImageCapture(Path.of(root.toString(), "20in.png"));
        pipeline.initPipeline(capture);
        var settings = new StandardCVPipelineSettings();

    }

    private void forceLoad() throws IOException {
        CameraServerJNI.forceLoad();
        CameraServerCvJNI.forceLoad();
    }

}
