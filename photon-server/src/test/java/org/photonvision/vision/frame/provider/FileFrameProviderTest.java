package org.photonvision.vision.frame.provider;

import static org.junit.jupiter.api.Assertions.*;

import edu.wpi.cscore.CameraServerCvJNI;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.frame.Frame;

public class FileFrameProviderTest {

    @BeforeAll
    public static void initPath() {

        try {
            CameraServerCvJNI.forceLoad();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void TestFilesExist() {
        assertTrue(Files.exists(TestUtils.getTestImagesPath()));
    }

    @Test
    public void Load2019ImageOnceTest() {
        var goodFilePath = TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in);

        assertTrue(Files.exists(goodFilePath));

        FileFrameProvider goodFrameProvider = new FileFrameProvider(goodFilePath, 68.5);

        Frame goodFrame = goodFrameProvider.get();

        int goodFrameCols = goodFrame.image.getMat().cols();
        int goodFrameRows = goodFrame.image.getMat().rows();

        // 2019 Images are at 320x240
        assertEquals(320, goodFrameCols);
        assertEquals(240, goodFrameRows);

        TestUtils.showImage(goodFrame.image.getMat(), "2019");

        var badFilePath = Paths.get("bad.jpg"); // this file does not exist

        FileFrameProvider badFrameProvider = null;

        try {
            badFrameProvider = new FileFrameProvider(badFilePath, 68.5);
        } catch (Exception e) {
            // ignored
        }

        assertNull(badFrameProvider);
    }

    @Test
    public void Load2020ImageOnceTest() {
        var goodFilePath = TestUtils.getWPIImagePath(TestUtils.WPI2020Image.kBlueGoal_108in_Center);

        assertTrue(Files.exists(goodFilePath));

        FileFrameProvider goodFrameProvider = new FileFrameProvider(goodFilePath, 68.5);

        Frame goodFrame = goodFrameProvider.get();

        int goodFrameCols = goodFrame.image.getMat().cols();
        int goodFrameRows = goodFrame.image.getMat().rows();

        // 2020 Images are at 640x480
        assertEquals(640, goodFrameCols);
        assertEquals(480, goodFrameRows);

        TestUtils.showImage(goodFrame.image.getMat(), "2020");

        var badFilePath = Paths.get("bad.jpg"); // this file does not exist

        FileFrameProvider badFrameProvider = null;

        try {
            badFrameProvider = new FileFrameProvider(badFilePath, 68.5);
        } catch (Exception e) {
            // ignored
        }

        assertNull(badFrameProvider);
    }
}
