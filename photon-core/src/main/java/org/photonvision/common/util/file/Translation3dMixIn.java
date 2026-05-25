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

package org.photonvision.common.util.file;

import io.avaje.jsonb.Json;
import org.wpilib.math.geometry.Translation3d;

@Json.MixIn(Translation3d.class)
public class Translation3dMixIn {
    @Json.Ignore(deserialize = true)
    @Json.Property("x")
    double m_x;

    @Json.Ignore(deserialize = true)
    @Json.Property("y")
    double m_y;

    @Json.Ignore(deserialize = true)
    @Json.Property("z")
    double m_z;

    @Json.Creator
    public static Translation3d construct(double m_x, double m_y, double m_z) {
        return new Translation3d(m_x, m_y, m_z);
    }
}
