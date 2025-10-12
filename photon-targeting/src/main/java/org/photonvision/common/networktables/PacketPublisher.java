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

package org.photonvision.common.networktables;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.networktables.RawPublisher;
import java.util.HashSet;
import java.util.Set;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.dataflow.structures.PacketSerde;

public class PacketPublisher<T> implements AutoCloseable {
    public final RawPublisher publisher;
    private final PacketSerde<T> photonStruct;

    public PacketPublisher(RawPublisher publisher, PacketSerde<T> photonStruct) {
        this.publisher = publisher;
        this.photonStruct = photonStruct;

        var mapper = new ObjectMapper();
        try {
            this.publisher
                    .getTopic()
                    .setProperty("message_uuid", mapper.writeValueAsString(photonStruct.getInterfaceUUID()));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        addSchemaImpl(photonStruct, new HashSet<>());
    }

    public void set(T value, int byteSize) {
        var packet = new Packet(byteSize);
        photonStruct.pack(packet, value);
        publisher.set(packet.getWrittenDataCopy());
    }

    public void set(T value) {
        set(value, photonStruct.getMaxByteSize());
    }

    @Override
    public void close() {
        publisher.close();
    }

    /**
     * Publish the schema for our type (and all nested types under it) to NT.
     *
     * <p>Copyright (c) FIRST and other WPILib contributors. Open Source Software; you can modify
     * and/or share it under the terms of the WPILib BSD license file in the root directory of this
     * project.
     *
     * @param struct The struct to publish
     * @param seen The set of types we've already published
     */
    private void addSchemaImpl(PacketSerde<?> struct, Set<String> seen) {
        var instance = this.publisher.getTopic().getInstance();

        String typeString = struct.getTypeString();

        if (instance.hasSchema(typeString)) {
            return;
        }

        if (!seen.add(typeString)) {
            throw new UnsupportedOperationException(typeString + ": circular reference with " + seen);
        }

        instance.addSchema(typeString, "photonstructschema", struct.getSchema());

        for (var inner : struct.getNestedPhotonMessages()) {
            addSchemaImpl(inner, seen);
        }
        for (var inner : struct.getNestedWpilibMessages()) {
            instance.addSchema(inner);
        }
        seen.remove(typeString);
    }
}
