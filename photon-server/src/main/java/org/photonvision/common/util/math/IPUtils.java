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
