/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.photonvision;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.photonvision.UnitTestUtils.waitForCondition;
import static org.photonvision.UnitTestUtils.waitForSequenceNumber;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTablesJNI;
import edu.wpi.first.wpilibj.Timer;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.jni.PhotonTargetingJniLoader;
import org.photonvision.jni.TimeSyncClient;
import org.photonvision.jni.WpilibLoader;
import org.photonvision.simulation.PhotonCameraSim;
import org.photonvision.targeting.PhotonPipelineResult;

class PhotonCameraTest {
    @BeforeAll
    public static void load_wpilib() {
        WpilibLoader.loadLibraries();
    }

    @BeforeEach
    public void setup() {
        HAL.initialize(500, 0);
    }

    @AfterEach
    public void teardown() {
        HAL.shutdown();
    }

    @Test
    public void testEmpty() {
        Assertions.assertDoesNotThrow(
                () -> {
                    var packet = new Packet(1);
                    var ret = new PhotonPipelineResult();
                    packet.setData(new byte[0]);
                    PhotonPipelineResult.photonStruct.pack(packet, ret);
                });
    }

    // Just a smoketest for dev use -- don't run by default
    @Test
    public void testTimeSyncServerWithPhotonCamera() throws InterruptedException, IOException {
        load_wpilib();
        PhotonTargetingJniLoader.load();

        HAL.initialize(500, 0);

        NetworkTableInstance.getDefault().stopClient();
        NetworkTableInstance.getDefault().startServer();

        var camera = new PhotonCamera("Arducam_OV2311_USB_Camera");
        PhotonCamera.setVersionCheckEnabled(false);

        for (int i = 0; i < 5; i++) {
            Thread.sleep(500);

            var res = camera.getLatestResult();
            var captureTime = res.getTimestampSeconds();
            var now = Timer.getFPGATimestamp();

            // expectTrue(captureTime < now);

            System.out.println(
                    "sequence "
                            + res.metadata.sequenceID
                            + " image capture "
                            + captureTime
                            + " received at "
                            + res.getTimestampSeconds()
                            + " now: "
                            + NetworkTablesJNI.now() / 1e6
                            + " time since last pong: "
                            + res.metadata.timeSinceLastPong / 1e6);
        }

        HAL.shutdown();
    }

    private static Stream<Arguments> testNtOffsets() {
        return Stream.of(
                // various initializaiton orders
                Arguments.of(1, 10, 30, 30),
                Arguments.of(10, 2, 30, 30),
                Arguments.of(10, 10, 30, 30),
                // Reboot just the robot
                Arguments.of(1, 1, 10, 30),
                // Reboot just the coproc
                Arguments.of(1, 1, 30, 10));
    }

    /**
     * Try starting client before server and vice-versa, making sure that we never fail the version
     * check
     */
    @ParameterizedTest
    @MethodSource("testNtOffsets")
    public void testRestartingRobotAndCoproc(
            int robotStart, int coprocStart, int robotRestart, int coprocRestart) throws Throwable {
        var robotNt = NetworkTableInstance.create();
        var coprocNt = NetworkTableInstance.create();

        robotNt.addLogger(10, 255, (it) -> System.out.println("ROBOT: " + it.logMessage.message));
        coprocNt.addLogger(10, 255, (it) -> System.out.println("CLIENT: " + it.logMessage.message));

        TimeSyncClient tspClient = null;

        var robotCamera = new PhotonCamera(robotNt, "MY_CAMERA");

        // apparently need a PhotonCamera to hand down
        var fakePhotonCoprocCam = new PhotonCamera(coprocNt, "MY_CAMERA");
        var coprocSim = new PhotonCameraSim(fakePhotonCoprocCam);
        coprocSim.prop.setCalibration(640, 480, Rotation2d.fromDegrees(90));
        coprocSim.prop.setFPS(30);
        coprocSim.setMinTargetAreaPixels(20.0);

        for (int i = 0; i < 20; i++) {
            int seq = i + 1;

            if (i == coprocRestart) {
                System.out.println("Restarting coprocessor NT client");

                fakePhotonCoprocCam.close();
                coprocNt.close();
                coprocNt = NetworkTableInstance.create();

                coprocNt.addLogger(10, 255, (it) -> System.out.println("CLIENT: " + it.logMessage.message));

                fakePhotonCoprocCam = new PhotonCamera(coprocNt, "MY_CAMERA");
                coprocSim = new PhotonCameraSim(fakePhotonCoprocCam);
                coprocSim.prop.setCalibration(640, 480, Rotation2d.fromDegrees(90));
                coprocSim.prop.setFPS(30);
                coprocSim.setMinTargetAreaPixels(20.0);
            }
            if (i == robotRestart) {
                System.out.println("Restarting robot NT server");

                robotNt.close();
                robotNt = NetworkTableInstance.create();
                robotNt.addLogger(10, 255, (it) -> System.out.println("ROBOT: " + it.logMessage.message));
                robotCamera = new PhotonCamera(robotNt, "MY_CAMERA");
            }

            if (i == coprocStart || i == coprocRestart) {
                coprocNt.setServer("127.0.0.1", 5940);
                coprocNt.startClient4("testClient");

                // PhotonCamera makes a server by default - connect to it
                tspClient = new TimeSyncClient("127.0.0.1", 5810, 0.5);
            }

            if (i == robotStart || i == robotRestart) {
                robotNt.startServer("networktables_random.json", "", 5941, 5940);
            }

            Thread.sleep(100);

            if (i == Math.max(coprocStart, robotStart)) {
                final var c = coprocNt;
                final var r = robotNt;
                waitForCondition("Coproc connection", () -> c.getConnections().length == 1);
                waitForCondition("Rio connection", () -> r.getConnections().length == 1);
            }

            var result1 = new PhotonPipelineResult();
            result1.metadata.captureTimestampMicros = seq * 100;
            result1.metadata.publishTimestampMicros = seq * 150;
            result1.metadata.sequenceID = seq;
            if (tspClient != null) {
                result1.metadata.timeSinceLastPong = tspClient.getPingMetadata().timeSinceLastPong();
            } else {
                result1.metadata.timeSinceLastPong = Long.MAX_VALUE;
            }

            coprocSim.submitProcessedFrame(result1, NetworkTablesJNI.now());
            coprocNt.flush();

            if (i > robotStart && i > coprocStart) {
                var ret = waitForSequenceNumber(robotCamera, seq);
                System.out.println(ret);
            }

            // force verifyVersion to do checks
            robotCamera.lastVersionCheckTime = -100;
            robotCamera.prevTimeSyncWarnTime = -100;
            assertDoesNotThrow(robotCamera::verifyVersion);
        }

        coprocSim.close();
        coprocNt.close();
        robotNt.close();
        tspClient.stop();
    }
}
