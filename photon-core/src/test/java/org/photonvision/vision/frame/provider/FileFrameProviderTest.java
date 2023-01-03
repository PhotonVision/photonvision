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

package org.photonvision.vision.frame.provider;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.common.util.TestUtils;
import org.photonvision.vision.frame.Frame;

public class FileFrameProviderTest {
    @BeforeAll
    public static void initPath() {
        TestUtils.loadLibraries();
    }

    @Test
    public void TestFilesExist() {
        assertTrue(Files.exists(TestUtils.getTestImagesPath(false)));
    }

    @Test
    public void Load2019ImageOnceTest() {
        var goodFilePath =
                TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoStraightDark72in, false);

        assertTrue(Files.exists(goodFilePath));

        FileFrameProvider goodFrameProvider = new FileFrameProvider(goodFilePath, 68.5);

        Frame goodFrame = goodFrameProvider.get();

        int goodFrameCols = goodFrame.colorImage.getMat().cols();
        int goodFrameRows = goodFrame.colorImage.getMat().rows();

        // 2019 Images are at 320x240
        assertEquals(320, goodFrameCols);
        assertEquals(240, goodFrameRows);

        TestUtils.showImage(goodFrame.colorImage.getMat(), "2019");

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
        var goodFilePath =
                TestUtils.getWPIImagePath(TestUtils.WPI2020Image.kBlueGoal_108in_Center, false);

        assertTrue(Files.exists(goodFilePath));

        FileFrameProvider goodFrameProvider = new FileFrameProvider(goodFilePath, 68.5);

        Frame goodFrame = goodFrameProvider.get();

        int goodFrameCols = goodFrame.colorImage.getMat().cols();
        int goodFrameRows = goodFrame.colorImage.getMat().rows();

        // 2020 Images are at 640x480
        assertEquals(640, goodFrameCols);
        assertEquals(480, goodFrameRows);

        TestUtils.showImage(goodFrame.colorImage.getMat(), "2020");

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
