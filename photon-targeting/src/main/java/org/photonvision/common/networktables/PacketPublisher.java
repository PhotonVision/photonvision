/*
 * MIT License
 *
 * Copyright (c) PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
