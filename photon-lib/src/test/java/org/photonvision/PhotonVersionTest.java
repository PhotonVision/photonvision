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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PhotonVersionTest {

    public static final boolean versionMatches(String versionString, String other) {
        String c = versionString;
        Pattern p = Pattern.compile("v[0-9]+.[0-9]+.[0-9]+");
        Matcher m = p.matcher(c);
        if (m.find()) {
            c = m.group(0);
        } else {
            return false;
        }
        m = p.matcher(other);
        if (m.find()) {
            other = m.group(0);
        } else {
            return false;
        }
        return c.equals(other);
    }

    @Test
    public void testVersion() {
        Assertions.assertTrue(versionMatches("v2021.1.6", "v2021.1.6"));
        Assertions.assertTrue(versionMatches("dev-v2021.1.6", "v2021.1.6"));
        Assertions.assertTrue(versionMatches("dev-v2021.1.6-5-gca49ea50", "v2021.1.6"));
        Assertions.assertFalse(versionMatches("", "v2021.1.6"));
        Assertions.assertFalse(versionMatches("v2021.1.6", ""));
    }
}
