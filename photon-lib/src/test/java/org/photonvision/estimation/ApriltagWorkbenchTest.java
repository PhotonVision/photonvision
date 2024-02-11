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

package org.photonvision.estimation;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;

public class ApriltagWorkbenchTest {
    @BeforeAll
    public static void setUp() {
        // No version check for testing
        PhotonCamera.setVersionCheckEnabled(false);
    }

    // @Test
    public void testMeme() throws IOException, InterruptedException {
        NetworkTableInstance instance = NetworkTableInstance.getDefault();
        instance.stopServer();
        // set the NT server if simulating this code.
        // "localhost" for photon on desktop, or "photonvision.local" / "[ip-address]"
        // for coprocessor
        instance.setServer("localhost");
        instance.startClient4("myRobot");

        var robotToCamera = new Transform3d();
        var cam = new PhotonCamera("WPI2023");
        var tagLayout =
                AprilTagFieldLayout.loadFromResource(AprilTagFields.k2023ChargedUp.m_resourceFile);

        var pe =
                new PhotonPoseEstimator(
                        tagLayout,
                        PhotonPoseEstimator.PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR,
                        cam,
                        robotToCamera);

        var field = new Field2d();
        SmartDashboard.putData(field);

        while (!Thread.interrupted()) {
            Thread.sleep(500);

            var ret = pe.update();
            System.out.println(ret);

            field.setRobotPose(ret.get().estimatedPose.toPose2d());
        }
    }
}
