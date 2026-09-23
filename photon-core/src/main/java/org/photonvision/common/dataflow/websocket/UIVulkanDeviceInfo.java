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

import io.avaje.jsonb.Json;
import org.photonvision.vkapriltag.VkAprilTagDeviceInfo;

/**
 * Wire-format copy of {@link VkAprilTagDeviceInfo}, so the websocket payload's shape is owned by
 * photon-core and doesn't change any time the vkapriltag_jni-java artifact's record does.
 */
@Json
public record UIVulkanDeviceInfo(int index, String name, String description, boolean isCpuDevice) {
    public static UIVulkanDeviceInfo from(VkAprilTagDeviceInfo device) {
        return new UIVulkanDeviceInfo(
                device.index(), device.name(), device.description(), device.isCpuDevice());
    }
}
