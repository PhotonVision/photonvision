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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

public class PathManagerTest {
    @Test
    public void testNamingFormat_LeadingZeros_And24HourUTC() {
        // GIVEN: a LocalDateTime with single-digit month, day, hour, minute, second
        LocalDateTime dt = LocalDateTime.of(2024, 1, 2, 3, 4, 5);

        // WHEN: taToLogFname is called
        String fname = PathManager.taToLogFname(dt);

        // THEN: the returned name has leading zeros in all date and time parts
        assertTrue(
                fname.matches("^photonvision-2024-01-02_03-04-05\\.log$"),
                "Filename should match the expected pattern with leading zeros");

        // THEN: time is in 24-hour format (03 not 03AM or 3PM), already shown implicitly
        assertTrue(fname.contains("_03-04-05"), "Time should be in 24-hour format with leading zeros");
    }

    @Test
    public void testNamingFormat_OtherDate_CheckPattern() {
        // GIVEN: another date and time
        LocalDateTime dt = LocalDateTime.of(2023, 12, 31, 23, 59, 59);

        // WHEN: taToLogFname is called
        String fname = PathManager.taToLogFname(dt);

        // THEN: the returned name correctly formats the entire timestamp
        assertEquals("photonvision-2023-12-31_23-59-59.log", fname);
    }

    @Test
    public void testNamingFormat_WithZonedDateTime_ConvertsToUTC() {
        // GIVEN: a UTC ZonedDateTime at 7:08:09 on June 3, 2024
        ZonedDateTime utcDateTime =
                ZonedDateTime.of(2024, Month.JUNE.getValue(), 3, 7, 8, 9, 0, ZoneId.of("UTC"));

        // WHEN: taToLogFname is called
        String fname = PathManager.taToLogFname(utcDateTime);

        // THEN: it is formatted in UTC, with leading zeros, 24-hour
        assertEquals("photonvision-2024-06-03_07-08-09.log", fname);
    }

    @Test
    public void testNamingFormat_WithNonUtcZoneTime_FormatsActualFieldsNotConverted() {
        // GIVEN: a ZonedDateTime for 20:17:16 America/New_York (should use the provided fields, not
        // convert to UTC)
        ZonedDateTime nyTime =
                ZonedDateTime.of(2024, 6, 3, 20, 17, 16, 0, ZoneId.of("America/New_York"));

        // WHEN: taToLogFname is called
        String fname = PathManager.taToLogFname(nyTime);

        // THEN: asserts that fields from the zoned time are respected as is
        assertEquals("photonvision-2024-06-03_20-17-16.log", fname);
    }
}
