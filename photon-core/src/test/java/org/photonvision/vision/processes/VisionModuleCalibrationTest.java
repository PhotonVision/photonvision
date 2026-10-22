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

package org.photonvision.vision.processes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.common.LoadJNI;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigProvider;
import org.photonvision.common.util.TestUtils;
import org.photonvision.jni.LibraryLoader;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.camera.FileVisionSource;
import org.photonvision.vision.camera.PVCameraInfo;
import org.photonvision.vision.camera.QuirkyCamera;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.frame.FrameThresholdType;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.pipeline.UICalibrationData;
import org.photonvision.vision.pipeline.UICalibrationData.BoardType;
import org.photonvision.vision.pipeline.UICalibrationData.TagFamily;
import org.wpilib.math.util.Units;

/**
 * Runs through the calibration functions added in {@link VisionModule}: starting a session,
 * computing pending calibrations, and committing or canceling.
 */
public class VisionModuleCalibrationTest {
    private static final Path DATASET_DIR =
            TestUtils.getCharucoBoardImagesPath().resolve("lifecam/2024-05-07_lifecam_1280");
    private static final Size RESOLUTION = new Size(1280, 720);
    private static final Size BOARD_SIZE = new Size(8, 8);
    private static final String PRELIMINARY_DIR_PREFIX = "photonvision_preliminary_";

    @BeforeAll
    public static void init() throws IOException {
        LoadJNI.loadLibraries();
        if (!LibraryLoader.loadTargeting()) fail();
        LoadJNI.forceLoad(LoadJNI.JNITypes.MRCAL);
        ConfigProvider.getInstance().load();
    }

    private VisionModule module;
    private PipelineManager pipelineManager;
    private CameraConfiguration config;

    private void setupModule(String cameraName) throws Exception {
        var cameraInfo =
                PVCameraInfo.fromFileInfo(
                        DATASET_DIR.resolve("img0.png").toAbsolutePath().toString(), cameraName);
        var source = new FileVisionSource(new CameraConfiguration(cameraInfo));
        config = source.getCameraConfiguration();
        pipelineManager = new PipelineManager(config);
        module = new VisionModule(pipelineManager, source, List.of());

        collectCalibrationSnapshots();
    }

    private void collectCalibrationSnapshots() throws IOException {
        var pipeline = pipelineManager.calibration3dPipeline;
        var settings = pipeline.getSettings();
        settings.boardType = BoardType.CHARUCOBOARD;
        settings.tagFamily = TagFamily.Dict_4X4_1000;
        settings.resolution = RESOLUTION;
        settings.boardHeight = (int) Math.round(BOARD_SIZE.height);
        settings.boardWidth = (int) Math.round(BOARD_SIZE.width);
        settings.gridSize = Units.inchesToMeters(1);
        settings.markerSize = Units.inchesToMeters(0.75);
        settings.streamingFrameDivisor = FrameDivisor.NONE;
        settings.useMrCal = true;
        settings.useOldPattern = false;

        File[] directoryListing = DATASET_DIR.toFile().listFiles();
        assertTrue(directoryListing.length >= 12);

        for (var file : directoryListing) {
            if (file.isFile()) {
                pipeline.takeSnapshot();
                var frame =
                        new Frame(
                                0,
                                new CVMat(Imgcodecs.imread(file.getAbsolutePath())),
                                new CVMat(),
                                FrameThresholdType.NONE,
                                new FrameStaticProperties(
                                        (int) RESOLUTION.width, (int) RESOLUTION.height, 67, null));
                try (var result = pipeline.run(frame, QuirkyCamera.DefaultCamera)) {}
            }
        }

        assertFalse(pipeline.foundCornersList.isEmpty());
        assertTrue(
                pipeline.foundCornersList.stream()
                        .map(it -> it.imagePoints)
                        .allMatch(it -> it.width() > 0 && it.height() > 0));
        assertTrue(
                pipeline.foundCornersList.stream()
                        .allMatch(
                                it ->
                                        it.imagePoints.width() == it.objectPoints.width()
                                                && it.imagePoints.height() == it.objectPoints.height()));
    }

    @AfterEach
    public void teardown() {
        if (module != null) module.close();
        module = null;
    }

    private static UICalibrationData calibrationStartData() {
        return new UICalibrationData(
                0,
                0,
                Units.inchesToMeters(1),
                Units.inchesToMeters(0.75),
                (int) BOARD_SIZE.width,
                (int) BOARD_SIZE.height,
                BoardType.CHARUCOBOARD,
                false,
                TagFamily.Dict_4X4_1000,
                false,
                null);
    }

    private void startCalibration() {
        module.startCalibration(calibrationStartData());
    }

    /**
     * The pipeline manager applies changes asynchronously so we poll until the mode switch lands (or
     * fail after a timeout).
     */
    private void awaitCalibrationMode(boolean inCalibration) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline
                && (pipelineManager.getCurrentPipelineIndex() == PipelineManager.CAL_3D_INDEX)
                        != inCalibration) {
            Thread.sleep(20);
        }
        if (inCalibration) {
            assertTrue(
                    pipelineManager.getCurrentPipelineIndex() == PipelineManager.CAL_3D_INDEX,
                    "Camera did not enter calibration mode");
        } else {
            assertFalse(
                    pipelineManager.getCurrentPipelineIndex() == PipelineManager.CAL_3D_INDEX,
                    "Camera did not leave calibration mode");
        }
    }

    private static void assertReleased(CameraCalibrationCoefficients coeffs) {
        assertThrows(RuntimeException.class, coeffs::getCameraIntrinsicsMat);
    }

    private static void assertNotReleased(CameraCalibrationCoefficients coeffs) {
        coeffs.getCameraIntrinsicsMat();
    }

    /** Preliminary calibration image dirs currently sitting in the system temp directory. */
    private List<Path> preliminaryImageDirs() throws IOException {
        try (var stream = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return stream
                    .filter(
                            p ->
                                    p.getFileName().toString().startsWith(PRELIMINARY_DIR_PREFIX + config.uniqueName))
                    .toList();
        }
    }

    private Path persistentImageDir() {
        var resolution = pipelineManager.calibration3dPipeline.getSettings().resolution;
        return ConfigProvider.getInstance()
                .getCalibrationImageSavePathWithRes(resolution, config.uniqueName);
    }

    /** The snapshot image from a compute must end up in the persistent calibration image dir. */
    private void assertSnapshotMovedToPersistentDir(CameraCalibrationCoefficients committed) {
        var observation = committed.getObservations().get(0);
        var moved = persistentImageDir().resolve(observation.snapshotName);
        assertTrue(Files.exists(moved), "Snapshot image was not moved to " + moved);
        assertEquals(
                moved,
                observation.snapshotDataLocation,
                "Observation does not point at the moved snapshot image");
    }

    @Test
    public void startThenCancel() throws Exception {
        setupModule("startThenCancel");

        startCalibration();
        awaitCalibrationMode(true);
        assertNull(module.getPendingCalibration());

        module.cancelCalibration();
        awaitCalibrationMode(false);

        assertNull(module.getPendingCalibration());
        assertTrue(config.calibrations.isEmpty(), "Cancel must not add a calibration");
        assertTrue(preliminaryImageDirs().isEmpty(), "Cancel must not compute a calibration");
    }

    @Test
    public void startThenComplete() throws Exception {
        setupModule("startThenComplete");

        startCalibration();
        awaitCalibrationMode(true);
        var committed = module.commitCalibration();

        assertNull(module.getPendingCalibration());
        assertEquals(1, config.calibrations.size());
        assertSame(committed, config.calibrations.get(0));
        assertNotReleased(committed);

        // The temporary image dir from the solve is cleaned up and its image moved to the
        // persistent calibration image dir
        assertTrue(preliminaryImageDirs().isEmpty());
        assertSnapshotMovedToPersistentDir(committed);
        awaitCalibrationMode(false);
    }

    @Test
    public void startPendingThenComplete() throws Exception {
        setupModule("startPendingThenComplete");

        startCalibration();
        awaitCalibrationMode(true);

        var pending = module.computeCalibration();
        assertSame(pending, module.getPendingCalibration());
        assertEquals(1, preliminaryImageDirs().size());

        var committed = module.commitCalibration();

        // Commit recomputes; the previously pending result is discarded in favor of the new solve
        assertNotSame(pending, committed);
        assertNull(module.getPendingCalibration());
        assertReleased(pending);

        assertEquals(1, config.calibrations.size());
        assertSame(committed, config.calibrations.get(0));
        assertNotReleased(committed);

        assertTrue(preliminaryImageDirs().isEmpty());
        assertSnapshotMovedToPersistentDir(committed);
    }

    @Test
    public void startPendingPendingThenComplete() throws Exception {
        setupModule("startPendingPendingThenComplete");

        startCalibration();
        awaitCalibrationMode(true);

        var firstPending = module.computeCalibration();
        var secondPending = module.computeCalibration();

        // The second solve replaces the first, releasing it and deleting its image dir
        assertSame(secondPending, module.getPendingCalibration());
        assertNotSame(firstPending, secondPending);
        assertReleased(firstPending);
        assertEquals(1, preliminaryImageDirs().size());

        var committed = module.commitCalibration();

        // Commit recomputes; both previously pending results are released
        assertNull(module.getPendingCalibration());
        assertNotSame(secondPending, committed);
        assertReleased(secondPending);

        assertEquals(1, config.calibrations.size());
        assertSame(committed, config.calibrations.get(0));
        assertNotReleased(committed);

        assertTrue(preliminaryImageDirs().isEmpty());
        assertSnapshotMovedToPersistentDir(committed);
    }

    @Test
    public void startPendingPendingThenCancel() throws Exception {
        setupModule("startPendingPendingThenCancel");

        startCalibration();
        awaitCalibrationMode(true);

        var firstPending = module.computeCalibration();
        var secondPending = module.computeCalibration();

        module.cancelCalibration();
        awaitCalibrationMode(false);

        assertNull(module.getPendingCalibration());
        assertReleased(firstPending);
        assertReleased(secondPending);
        assertTrue(config.calibrations.isEmpty(), "Cancel must not add a calibration");
        assertTrue(preliminaryImageDirs().isEmpty());
        var persistentDir = persistentImageDir();
        try (var stream = Files.list(persistentDir)) {
            assertTrue(
                    stream.findAny().isEmpty(), "Cancel must not leave snapshot images in " + persistentDir);
        }
    }

    @Test
    public void startPendingThenCancel() throws Exception {
        setupModule("startPendingThenCancel");

        startCalibration();
        awaitCalibrationMode(true);

        var pending = module.computeCalibration();
        assertSame(pending, module.getPendingCalibration());

        module.cancelCalibration();
        awaitCalibrationMode(false);

        assertNull(module.getPendingCalibration());
        assertReleased(pending);
        assertTrue(config.calibrations.isEmpty(), "Cancel must not add a calibration");
        assertTrue(preliminaryImageDirs().isEmpty());
        var persistentDir = persistentImageDir();
        try (var stream = Files.list(persistentDir)) {
            assertTrue(
                    stream.findAny().isEmpty(), "Cancel must not leave snapshot images in " + persistentDir);
        }
    }
}
