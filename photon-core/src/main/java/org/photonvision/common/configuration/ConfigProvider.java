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

package org.photonvision.common.configuration;

import java.nio.file.Path;

public abstract class ConfigProvider {
    protected PhotonConfiguration config;

    abstract void load();

    abstract boolean saveToDisk();

    PhotonConfiguration getConfig() {
        return config;
    }

    public void clearConfig() {
        config = new PhotonConfiguration();
    }

    public abstract boolean saveUploadedHardwareConfig(Path uploadPath);

    public abstract boolean saveUploadedHardwareSettings(Path uploadPath);

    public abstract boolean saveUploadedNetworkConfig(Path uploadPath);

    public abstract boolean saveUploadedAprilTagFieldLayout(Path uploadPath);
}
