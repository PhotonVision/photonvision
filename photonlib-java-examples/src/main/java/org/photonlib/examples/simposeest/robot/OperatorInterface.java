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

import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.SlewRateLimiter;
import edu.wpi.first.wpilibj.XboxController;

public class OperatorInterface {
    private XboxController opCtrl = new XboxController(0);

    // Slew rate limiters to make joystick inputs more gentle; 1/3 sec from 0
    // to 1.
    private SlewRateLimiter speedLimiter = new SlewRateLimiter(3);
    private SlewRateLimiter rotLimiter = new SlewRateLimiter(3);

    public OperatorInterface() {}

    public double getFwdRevSpdCmd() {
        // Get the x speed. We are inverting this because Xbox controllers return
        // negative values when we push forward.
        return -speedLimiter.calculate(opCtrl.getY(GenericHID.Hand.kLeft)) * Constants.kMaxSpeed;
    }

    public double getRotateSpdCmd() {
        // Get the rate of angular rotation. We are inverting this because we want a
        // positive value when we pull to the left (remember, CCW is positive in
        // mathematics). Xbox controllers return positive values when you pull to
        // the right by default.
        return -rotLimiter.calculate(opCtrl.getX(GenericHID.Hand.kRight)) * Constants.kMaxAngularSpeed;
    }

    public boolean getSimKickCmd() {
        return opCtrl.getXButtonPressed();
    }
}
