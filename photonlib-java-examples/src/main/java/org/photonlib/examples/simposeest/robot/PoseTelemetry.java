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

package org.photonlib.examples.simposeest.robot;

import edu.wpi.first.wpilibj.geometry.Pose2d;
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
