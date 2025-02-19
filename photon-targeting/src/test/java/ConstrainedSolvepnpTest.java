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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.MatBuilder;
import edu.wpi.first.math.Nat;
import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.photonvision.jni.ConstrainedSolvepnpJni;
import org.photonvision.jni.PhotonTargetingJniLoader;
import org.photonvision.jni.WpilibLoader;

public class ConstrainedSolvepnpTest {
    @BeforeAll
    public static void load_wpilib() throws UnsatisfiedLinkError, IOException {
        if (!WpilibLoader.loadLibraries()) {
            fail();
        }
        if (!PhotonTargetingJniLoader.load()) {
            fail();
        }

        HAL.initialize(1000, 0);
    }

    @AfterAll
    public static void teardown() {
        HAL.shutdown();
    }

    @Test
    public void smoketest() {
        double[] cameraCal =
                new double[] {
                    600, 600, 300, 150,
                };

        var field2points =
                MatBuilder.fill(
                                Nat.N4(),
                                Nat.N4(),
                                2.5,
                                0 - 0.08255,
                                0.5 - 0.08255,
                                1,
                                2.5,
                                0 - 0.08255,
                                0.5 + 0.08255,
                                1,
                                2.5,
                                0 + 0.08255,
                                0.5 + 0.08255,
                                1,
                                2.5,
                                0 + 0.08255,
                                0.5 - 0.08255,
                                1)
                        .transpose();

        var point_observations =
                MatBuilder.fill(Nat.N4(), Nat.N2(), 333, -17, 333, -83, 267, -83, 267, -17).transpose();

        // Camera with +x in world -y, +y in world -z, and +z in world +x
        // (IE, camera pointing straight along the +X axis facing forwards)
        var robot2camera =
                MatBuilder.fill(Nat.N4(), Nat.N4(), 0, 0, 1, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 1);

        // Initial guess for optimization
        double[] x_guess = new double[] {0.2, 0.1, -.05};

        var ret =
                ConstrainedSolvepnpJni.do_optimization(
                        true,
                        1,
                        cameraCal,
                        robot2camera.getData(),
                        x_guess,
                        field2points.getData(),
                        point_observations.getData(),
                        0,
                        0);
        assertNotNull(ret);
        System.out.println(Arrays.toString(ret));
    }
}
