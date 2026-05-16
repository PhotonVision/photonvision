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

package org.photonvision;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.targeting.*;
import org.photonvision.targeting.serde.PhotonStructSerializable;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;

public class SerdeTest {
    private <T extends PhotonStructSerializable<T>> boolean testSerde(T data) {
        var p = new Packet(10);
        p.encode(data);
        var unpackedData = p.decode(data); // kinda scuffed lowkey
        return data.equals(unpackedData);
    }

    @Test
    public void int8Test() {
        // Default test
        var test1 = new Int8TestMessage();
        assertTrue(testSerde(test1));
        // Optional test
        var test2 = new Int8TestMessage();
        test2.optTest = Optional.of((byte) 3);
        assertTrue(testSerde(test2));
        // VLA test
        var test3 = new Int8TestMessage();
        test3.vlaTest = List.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(testSerde(test3));
        // General test
        var test4 = new Int8TestMessage();
        test4.test = (byte) 1;
        test4.vlaTest = List.of((byte) 1, (byte) 2, (byte) 3);
        test4.optTest = Optional.of((byte) 3);
        assertTrue(testSerde(test4));
    }

    @Test
    public void int16Test() {
        // Default test
        var test1 = new Int16TestMessage();
        assertTrue(testSerde(test1));
        // Optional test
        var test2 = new Int16TestMessage();
        test2.optTest = Optional.of((short) 3);
        assertTrue(testSerde(test2));
        // VLA test
        var test3 = new Int16TestMessage();
        test3.vlaTest = List.of((short) 1, (short) 2, (short) 3);
        assertTrue(testSerde(test3));
        // General test
        var test4 = new Int16TestMessage();
        test4.test = (short) 1;
        test4.vlaTest = List.of((short) 1, (short) 2, (short) 3);
        test4.optTest = Optional.of((short) 3);
        assertTrue(testSerde(test4));
    }

    @Test
    public void int32Test() {
        // Default test
        var test1 = new Int32TestMessage();
        assertTrue(testSerde(test1));
        // Optional test
        var test2 = new Int32TestMessage();
        test2.optTest = Optional.of((int) 3);
        assertTrue(testSerde(test2));
        // VLA test
        var test3 = new Int32TestMessage();
        test3.vlaTest = List.of((int) 1, (int) 2, (int) 3);
        assertTrue(testSerde(test3));
        // General test
        var test4 = new Int32TestMessage();
        test4.test = (int) 1;
        test4.vlaTest = List.of((int) 1, (int) 2, (int) 3);
        test4.optTest = Optional.of((int) 3);
        assertTrue(testSerde(test4));
    }

    @Test
    public void int64Test() {
        // Default test
        var test1 = new Int64TestMessage();
        assertTrue(testSerde(test1));
        // Optional test
        var test2 = new Int64TestMessage();
        test2.optTest = Optional.of((long) 3);
        assertTrue(testSerde(test2));
        // VLA test
        var test3 = new Int64TestMessage();
        test3.vlaTest = List.of((long) 1, (long) 2, (long) 3);
        assertTrue(testSerde(test3));
        // General test
        var test4 = new Int64TestMessage();
        test4.test = (long) 1;
        test4.vlaTest = List.of((long) 1, (long) 2, (long) 3);
        test4.optTest = Optional.of((long) 3);
        assertTrue(testSerde(test4));
    }

    @Test
    public void float32Test() {
        // Default test
        var test1 = new Float32TestMessage();
        assertTrue(testSerde(test1));
        // Optional test
        var test2 = new Float32TestMessage();
        test2.optTest = Optional.of((float) 3.0);
        assertTrue(testSerde(test2));
        // VLA test
        var test3 = new Float32TestMessage();
        test3.vlaTest = List.of((float) 1.0, (float) 2.0, (float) 3.0);
        assertTrue(testSerde(test3));
        // General test
        var test4 = new Float32TestMessage();
        test4.test = (float) 1.0;
        test4.vlaTest = List.of((float) 1.0, (float) 2.0, (float) 3.0);
        test4.optTest = Optional.of((float) 3.0);
        assertTrue(testSerde(test4));
    }

    @Test
    public void float64Test() {
        // Default test
        var test1 = new Float64TestMessage();
        assertTrue(testSerde(test1));
        // Optional test
        var test2 = new Float64TestMessage();
        test2.optTest = Optional.of((double) 3.0);
        assertTrue(testSerde(test2));
        // VLA test
        var test3 = new Float64TestMessage();
        test3.vlaTest = List.of((double) 1.0, (double) 2.0, (double) 3.0);
        assertTrue(testSerde(test3));
        // General test
        var test4 = new Float64TestMessage();
        test4.test = (float) 1.0;
        test4.vlaTest = List.of((double) 1.0, (double) 2.0, (double) 3.0);
        test4.optTest = Optional.of((double) 3.0);
        assertTrue(testSerde(test4));
    }

    @Test
    public void boolTest() {
        // Default test
        var test1 = new BoolTestMessage();
        assertTrue(testSerde(test1));
        // Optional test
        var test2 = new BoolTestMessage();
        test2.optTest = Optional.of(true);
        assertTrue(testSerde(test2));
        // VLA test
        var test3 = new BoolTestMessage();
        test3.vlaTest = List.of(true, false, true);
        assertTrue(testSerde(test3));
        // General test
        var test4 = new BoolTestMessage();
        test4.test = true;
        test4.vlaTest = List.of(true, false, true);
        test4.optTest = Optional.of(true);
        assertTrue(testSerde(test4));
    }

    @Test
    public void Transform3dTest() {
        // Default test
        var test1 = new Transform3dTestMessage();
        assertTrue(testSerde(test1));
        // Optional test
        var test2 = new Transform3dTestMessage();
        test2.optTest = Optional.of(new Transform3d(1.0, 2.0, 3.0, new Rotation3d(1.0, 2.0, 3.0)));
        assertTrue(testSerde(test2));
        // VLA test
        var test3 = new Transform3dTestMessage();
        test3.vlaTest =
                List.of(
                        new Transform3d(1.0, 2.0, 3.0, new Rotation3d(1.0, 2.0, 3.0)),
                        new Transform3d(2.0, 1.0, 5.0, new Rotation3d(4.0, 3.0, 2.0)),
                        new Transform3d(1.0, 0.0, 0.0, new Rotation3d(1.0, 5.0, 3.0)));
        assertTrue(testSerde(test3));
        // General test
        var test4 = new Transform3dTestMessage();
        test4.test = new Transform3d(0.0, 1.0, 0.0, new Rotation3d(0.0, 2.0, 0.0));
        test4.vlaTest =
                List.of(
                        new Transform3d(1.0, 2.0, 3.0, new Rotation3d(1.0, 2.0, 3.0)),
                        new Transform3d(2.0, 1.0, 5.0, new Rotation3d(4.0, 3.0, 2.0)),
                        new Transform3d(1.0, 0.0, 0.0, new Rotation3d(1.0, 5.0, 3.0)));
        test4.optTest = Optional.of(new Transform3d(1.0, 2.0, 3.0, new Rotation3d(1.0, 2.0, 3.0)));
        assertTrue(testSerde(test4));
    }
}
