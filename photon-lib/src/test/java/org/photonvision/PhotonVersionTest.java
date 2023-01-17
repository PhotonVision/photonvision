/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
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
