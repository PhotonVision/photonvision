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

package org.photonvision.common.util.math;

import java.util.ArrayList;
import java.util.List;

public class IPUtils {
    public static boolean isValidIPV4(final String ip) {
        String PATTERN =
                "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

        return ip.matches(PATTERN);
    }

    public static List<Byte> getDigitBytes(int num) {
        List<Byte> digits = new ArrayList<>();
        collectDigitBytes(num, digits);
        return digits;
    }

    private static void collectDigitBytes(int num, List<Byte> digits) {
        if (num / 10 > 0) {
            collectDigitBytes(num / 10, digits);
        }
        digits.add((byte) (num % 10));
    }

    public static List<Integer> getDigits(int num) {
        List<Integer> digits = new ArrayList<>();
        collectDigits(num, digits);
        return digits;
    }

    private static void collectDigits(int num, List<Integer> digits) {
        if (num / 10 > 0) {
            collectDigits(num / 10, digits);
        }
        digits.add(num % 10);
    }
}
