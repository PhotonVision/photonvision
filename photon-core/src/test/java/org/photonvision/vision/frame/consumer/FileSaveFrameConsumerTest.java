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

package org.photonvision.vision.frame.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.MatchType;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.networktables.NetworkTablesManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.jni.LibraryLoader;
import org.photonvision.vision.frame.provider.FileFrameProvider;

public class FileSaveFrameConsumerTest {
    NetworkTableInstance inst = null;

    @BeforeAll
    public static void init() throws IOException {
        if (!LibraryLoader.loadWpiLibraries()) {
            fail();
        }

        if (!LibraryLoader.loadTargeting()) {
            fail();
        }
    }

    @BeforeEach
    public void setup() {
        assertNull(inst);

        HAL.initialize(500, 0);

        inst = NetworkTablesManager.getInstance().getNTInst();
        inst.stopClient();
        inst.stopServer();
        inst.startLocal();
        SmartDashboard.setNetworkTableInstance(inst);

        // DriverStation uses the default instance internally
        assertEquals(NetworkTableInstance.getDefault(), inst);
    }

    @AfterEach
    public void teardown() {
        SimHooks.resumeTiming();

        HAL.shutdown();
    }

    @CartesianTest
    public void testNoMatch(
            @Enum(MatchType.class) MatchType matchType, @Values(ints = {0, 1, 0xffff}) int matchNumber) {
        String camNickname = "foobar";
        String cameraUniqueName = "some_unique";
        String streamPrefix = "input";

        // GIVEN an input consumer
        FileSaveFrameConsumer consumer =
                new FileSaveFrameConsumer(camNickname, cameraUniqueName, streamPrefix);

        // AND a frameProvider giving a random test mode image
        var frameProvider =
                new FileFrameProvider(
                        TestUtils.getWPIImagePath(TestUtils.WPI2019Image.kCargoSideStraightDark72in, false),
                        TestUtils.WPI2019Image.FOV);

        // AND fake FMS data
        String eventName = "CASJ";
        DriverStationSim.setMatchType(matchType);
        DriverStationSim.setMatchNumber(matchNumber);
        DriverStationSim.setEventName(eventName);
        DriverStation.refreshData();

        // WHEN we save the image
        var currentTime = new Date();
        var counterPublisher = consumer.saveFrameEntry.getTopic().publish();
        counterPublisher.accept(1);
        consumer.accept(frameProvider.get().colorImage, currentTime);

        // THEN an image will be created on disk
        File expectedSnapshot =
                ConfigManager.getInstance()
                        .getImageSavePath()
                        .resolve(cameraUniqueName)
                        .resolve(
                                camNickname
                                        + "_"
                                        + streamPrefix
                                        + "_"
                                        + FileSaveFrameConsumer.df.format(currentTime)
                                        + "T"
                                        + FileSaveFrameConsumer.tf.format(currentTime)
                                        + "_"
                                        + (matchType.name() + "-" + matchNumber + "-" + eventName)
                                        + FileSaveFrameConsumer.FILE_EXTENSION)
                        .toFile();
        System.out.println("Checking that file exists: " + expectedSnapshot.getAbsolutePath());
        assertTrue(expectedSnapshot.exists());
    }
}
