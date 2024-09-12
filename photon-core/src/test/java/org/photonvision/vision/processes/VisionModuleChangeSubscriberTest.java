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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.photonvision.vision.processes.VisionModuleChangeSubscriber.setProperty;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.photonvision.common.util.numbers.DoubleCouple;
import org.photonvision.common.util.numbers.IntegerCouple;

public class VisionModuleChangeSubscriberTest {
    enum TestEnum {
        VALUE1,
        VALUE2
    }

    static class TestClass {
        public TestEnum enumField;
        public DoubleCouple doubleCoupleField;
        public IntegerCouple integerCoupleField;
        public double doubleField;
        public int intField;
        public boolean booleanField;
        public String stringField;

        public TestClass() {
            enumField = TestEnum.VALUE1;
            doubleCoupleField = new DoubleCouple(0, 0);
            integerCoupleField = new IntegerCouple(0, 0);
            doubleField = 0;
            intField = 0;
            booleanField = false;
            stringField = "";
        }
    }

    @Test
    // Either set with the enum Variant or the ordinal value
    void testSetEnumField() throws Exception {
        TestClass obj = new TestClass();
        assertEquals(obj.enumField, TestEnum.VALUE1);

        setProperty(obj, "enumField", TestEnum.VALUE2.ordinal());
        assertEquals(TestEnum.VALUE2, obj.enumField);
    }

    @Test
    void testSetDoubleCoupleField() throws Exception {
        TestClass obj = new TestClass();
        assertEquals(new DoubleCouple(0, 0), obj.doubleCoupleField);

        ArrayList<Number> values = new ArrayList<>();
        values.add(1.1);
        values.add(2.2);

        setProperty(obj, "doubleCoupleField", values);

        assertEquals(1.1, obj.doubleCoupleField.getFirst());
        assertEquals(2.2, obj.doubleCoupleField.getSecond());
    }

    @Test
    void testSetIntegerCoupleField() throws Exception {
        TestClass obj = new TestClass();
        assertEquals(new IntegerCouple(0, 0), obj.integerCoupleField);

        ArrayList<Number> values = new ArrayList<>();
        values.add(1);
        values.add(2);

        setProperty(obj, "integerCoupleField", values);

        assertEquals(1, obj.integerCoupleField.getFirst());
        assertEquals(2, obj.integerCoupleField.getSecond());
    }

    @Test
    void testSetDoubleField() throws Exception {
        TestClass obj = new TestClass();
        assertEquals(0, obj.doubleField);

        setProperty(obj, "doubleField", 3.14);
        assertEquals(3.14, obj.doubleField);
    }

    @Test
    void testSetIntField() throws Exception {
        TestClass obj = new TestClass();
        assertEquals(0, obj.intField);

        setProperty(obj, "intField", 42);
        assertEquals(42, obj.intField);
    }

    @Test
    void testSetBooleanField() throws Exception {
        TestClass obj = new TestClass();
        assertEquals(false, obj.booleanField);

        setProperty(obj, "booleanField", 1);
        assertTrue(obj.booleanField);

        setProperty(obj, "booleanField", 0);
        assertFalse(obj.booleanField);
    }

    @Test
    void testSetStringField() throws Exception {
        TestClass obj = new TestClass();
        assertEquals("", obj.stringField);

        setProperty(obj, "stringField", "test");
        assertEquals("test", obj.stringField);
    }

    @Test
    void testSetNonExistentField() {
        TestClass obj = new TestClass();
        Executable executable = () -> setProperty(obj, "nonExistentField", 1);
        assertThrows(NoSuchFieldException.class, executable);
    }

    @Test
    void testSetFieldWithIncompatibleType() {
        TestClass obj = new TestClass();
        Executable executable = () -> setProperty(obj, "doubleField", "string");
        assertThrows(Exception.class, executable);
    }
}
