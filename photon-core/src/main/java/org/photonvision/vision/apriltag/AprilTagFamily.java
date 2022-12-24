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

package org.photonvision.vision.apriltag;

public enum AprilTagFamily {
    kTag36h11,
    kTag25h9,
    kTag16h5,
    kTagCircle21h7,
    kTagCircle49h12,
    kTagStandard41h12,
    kTagStandard52h13,
    kTagCustom48h11;

    public String getNativeName() {
        // We wanna strip the leading kT and replace with "t"
        return this.name().replaceFirst("kT", "t");
    }
}
