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

package org.photonvision.common.dataflow.websocket;

import edu.wpi.first.apriltag.AprilTagFieldLayout;

public class UIProgramSettings {
    public UIProgramSettings(
            UINetConfig networkSettings,
            UILightingConfig lighting,
            UIGeneralSettings general,
            AprilTagFieldLayout atfl) {
        this.networkSettings = networkSettings;
        this.lighting = lighting;
        this.general = general;
        this.atfl = atfl;
    }

    public UINetConfig networkSettings;
    public UILightingConfig lighting;
    public UIGeneralSettings general;
    public AprilTagFieldLayout atfl;
}
