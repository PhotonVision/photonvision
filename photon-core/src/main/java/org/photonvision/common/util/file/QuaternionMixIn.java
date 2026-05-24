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
import org.wpilib.math.geometry.Quaternion;

@Json.MixIn(Quaternion.class)
public class QuaternionMixIn {
    @Json.Ignore(deserialize = true)
    @Json.Property("W")
    double m_w;

    @Json.Ignore(deserialize = true)
    @Json.Property("X")
    double m_x;

    @Json.Ignore(deserialize = true)
    @Json.Property("Y")
    double m_y;

    @Json.Ignore(deserialize = true)
    @Json.Property("Z")
    double m_z;

    @Json.Creator
    public static Quaternion construct(double m_w, double m_x, double m_y, double m_z) {
        return new Quaternion(m_w, m_x, m_y, m_z);
    }
}
