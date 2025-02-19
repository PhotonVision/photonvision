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

package org.photonvision.jni;

public class ConstrainedSolvepnpJni {
    public static native double[] do_optimization(
            boolean heading_free,
            int nTags,
            double[] cameraCal,
            double[] robot2camera,
            double[] x_guess,
            double[] field2points,
            double[] point_observations,
            double gyro_θ,
            double gyro_error_scale_fac);
}
