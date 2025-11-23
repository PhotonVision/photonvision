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

package org.photonvision.common.hardware;

public enum PhotonStatus {
    // Nominal states
    NT_CONNECTED_TARGETS_VISIBLE,
    NT_CONNECTED_TARGETS_MISSING,
    NT_DISCONNECTED_TARGETS_VISIBLE,
    NT_DISCONNECTED_TARGETS_MISSING,

    // Error states
    GENERIC_ERROR(true);

    final boolean error;

    PhotonStatus() {
        this(false);
    }

    PhotonStatus(boolean error) {
        this.error = error;
    }

    boolean isError() {
        return this.error;
    }
}
