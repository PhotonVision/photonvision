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

package frc.robot;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/** Reports our expected, desired, and actual poses to dashboards */
public class PoseTelemetry {
    Field2d field = new Field2d();

    Pose2d actPose = new Pose2d();
    Pose2d desPose = new Pose2d();
    Pose2d estPose = new Pose2d();

    public PoseTelemetry() {
        SmartDashboard.putData("Field", field);
        update();
    }

    public void update() {
        field.getObject("DesPose").setPose(desPose);
        field.getObject("ActPose").setPose(actPose);
        field.getObject("Robot").setPose(estPose);
    }

    public void setActualPose(Pose2d in) {
        actPose = in;
    }

    public void setDesiredPose(Pose2d in) {
        desPose = in;
    }

    public void setEstimatedPose(Pose2d in) {
        estPose = in;
    }
}
