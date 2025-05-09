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
