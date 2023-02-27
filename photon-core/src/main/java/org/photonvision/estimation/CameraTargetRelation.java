/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
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

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform3d;

/** Holds various helper geometries describing the relation between camera and target. */
public class CameraTargetRelation {
    public final Pose3d camPose;
    public final Transform3d camToTarg;
    public final double camToTargDist;
    public final double camToTargDistXY;
    public final Rotation2d camToTargYaw;
    public final Rotation2d camToTargPitch;
    /** Angle from the camera's relative x-axis */
    public final Rotation2d camToTargAngle;

    public final Transform3d targToCam;
    public final Rotation2d targToCamYaw;
    public final Rotation2d targToCamPitch;
    /** Angle from the target's relative x-axis */
    public final Rotation2d targToCamAngle;

    public CameraTargetRelation(Pose3d cameraPose, Pose3d targetPose) {
        this.camPose = cameraPose;
        camToTarg = new Transform3d(cameraPose, targetPose);
        camToTargDist = camToTarg.getTranslation().getNorm();
        camToTargDistXY =
                Math.hypot(camToTarg.getTranslation().getX(), camToTarg.getTranslation().getY());
        camToTargYaw = new Rotation2d(camToTarg.getX(), camToTarg.getY());
        camToTargPitch = new Rotation2d(camToTargDistXY, -camToTarg.getZ());
        camToTargAngle =
                new Rotation2d(Math.hypot(camToTargYaw.getRadians(), camToTargPitch.getRadians()));
        targToCam = new Transform3d(targetPose, cameraPose);
        targToCamYaw = new Rotation2d(targToCam.getX(), targToCam.getY());
        targToCamPitch = new Rotation2d(camToTargDistXY, -targToCam.getZ());
        targToCamAngle =
                new Rotation2d(Math.hypot(targToCamYaw.getRadians(), targToCamPitch.getRadians()));
    }
}
