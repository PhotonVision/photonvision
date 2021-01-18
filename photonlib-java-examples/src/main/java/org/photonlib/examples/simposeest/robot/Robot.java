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

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.kinematics.ChassisSpeeds;
import org.photonlib.examples.simposeest.sim.DrivetrainSim;

public class Robot extends TimedRobot {

    AutoController autoCtrl = new AutoController();
    Drivetrain dt = new Drivetrain();
    OperatorInterface opInf = new OperatorInterface();

    DrivetrainSim dtSim = new DrivetrainSim();

    PoseTelemetry pt = new PoseTelemetry();

    @Override
    public void robotInit() {
        // Flush NetworkTables every loop. This ensures that robot pose and other values
        // are sent during every iteration.
        setNetworkTablesFlushEnabled(true);
    }

    @Override
    public void autonomousInit() {
        resetOdometery();
        autoCtrl.startPath();
    }

    @Override
    public void autonomousPeriodic() {
        ChassisSpeeds speeds = autoCtrl.getCurMotorCmds(dt.getCtrlsPoseEstimate());
        dt.drive(speeds.vxMetersPerSecond, speeds.omegaRadiansPerSecond);
        pt.setDesiredPose(autoCtrl.getCurPose2d());
    }

    @Override
    public void teleopPeriodic() {
        dt.drive(opInf.getFwdRevSpdCmd(), opInf.getRotateSpdCmd());
    }

    @Override
    public void robotPeriodic() {
        pt.setEstimatedPose(dt.getCtrlsPoseEstimate());
        pt.update();
    }

    @Override
    public void disabledPeriodic() {
        dt.drive(0, 0);
    }

    @Override
    public void simulationPeriodic() {
        if (opInf.getSimKickCmd()) {
            dtSim.applyKick();
        }
        dtSim.update();
        pt.setActualPose(dtSim.getCurPose());
    }

    private void resetOdometery() {
        Pose2d startPose = autoCtrl.getInitialPose();
        dtSim.resetPose(startPose);
        dt.resetOdometry(startPose);
    }
}
