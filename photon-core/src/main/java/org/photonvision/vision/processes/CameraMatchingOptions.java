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

package org.photonvision.vision.processes;

import java.util.List;
import org.photonvision.vision.camera.CameraType;

public class CameraMatchingOptions {
    public CameraMatchingOptions(
            boolean checkUSBPath,
            boolean checkVidPid,
            boolean checkBaseName,
            boolean checkPath,
            CameraType... allowedTypes) {
        this.checkUSBPath = checkUSBPath;
        this.checkVidPid = checkVidPid;
        this.checkBaseName = checkBaseName;
        this.checkPath = checkPath;
        this.allowedTypes = List.of(allowedTypes);
    }

    public final boolean checkUSBPath;
    public final boolean checkVidPid;
    public final boolean checkBaseName;
    public final boolean checkPath;
    public final List<CameraType> allowedTypes;

    @Override
    public String toString() {
        return "CameraMatchingOptions [checkUSBPath="
                + checkUSBPath
                + ", checkVidPid="
                + checkVidPid
                + ", checkBaseName="
                + checkBaseName
                + ", checkPath="
                + checkPath
                + ", allowedTypes="
                + allowedTypes
                + "]";
    }
}
