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

package org.photonvision.vision.camera;

public enum CameraQuirk {
    /** Camera settable for controllable image gain */
    Gain,
    /** For the Raspberry Pi Camera */
    PiCam,
    /** Cap at 100FPS for high-bandwidth cameras */
    FPSCap100,
    /** Separate red/blue gain controls available */
    AWBGain,
    /** Will not work with photonvision - Logitec C270 at least */
    CompletelyBroken,
    /** Has adjustable focus and autofocus switch */
    AdjustableFocus,
}
