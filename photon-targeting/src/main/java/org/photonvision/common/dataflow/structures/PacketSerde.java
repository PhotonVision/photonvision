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

package org.photonvision.common.dataflow.structures;

import edu.wpi.first.util.struct.Struct;

public interface PacketSerde<T> {
    int getMaxByteSize();

    void pack(Packet packet, T value);

    T unpack(Packet packet);

    /** The name of this struct (eg "PhotonTrackedTarget") */
    String getTypeName();

    /**
     * Gets the type string (e.g. for NetworkTables). This should be globally unique and start with
     * "photonstruct:".
     *
     * @return type string
     */
    default String getTypeString() {
        return "photonstruct:" + getTypeName() + ":" + getInterfaceUUID();
    }

    /** Gets the list of photonstruct types referenced by this struct. */
    default PacketSerde<?>[] getNestedPhotonMessages() {
        return new PacketSerde<?>[] {};
    }

    /** Gets the list of WPILib struct types referenced by this struct. */
    default Struct<?>[] getNestedWpilibMessages() {
        return new Struct<?>[] {};
    }

    /** The schema definition, as defined in photon-serde/README.md */
    String getSchema();

    /** The hash of the schema string */
    String getInterfaceUUID();
}
