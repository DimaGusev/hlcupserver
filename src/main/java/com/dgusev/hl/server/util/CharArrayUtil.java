package com.dgusev.hl.server.util;

/**
 * Created by dgusev on 06.10.2017.
 */
public class CharArrayUtil {

    public static int indexOf(char[] buf, int start, int end, char needle) {
        for (int i = start; i< end; i++) {
            if (buf[i] == needle) {
                return i;
            }
        }
        return -1;
    }

    public static String createString(char[] buf, int start, int end) {
        return new String(buf, start, end - start);
    }

    public static boolean equals(char[] buf, int start, int end, char[] example) {
        if (example.length != end - start) {
            return false;
        }
        for (int i = start; i< end; i++) {
            if (buf[i] != example[i - start]) {
                return false;
            }
        }
        return true;
    }

    public static int parsePositiveInteger(char[] buf, int start, int end) {
        int result = 0;
        int count = end - start;
        if (count < 1) {
            return -1;
        }
        for (int i = start; i< end; i++) {
            int digit = buf[i] - 48;
            if (digit < 0 || digit > 9 ) {
                return -1;
            }
            result *= 10;
            result -= digit;
        }
        return -result;
    }

    public static long parseLong(char[] buf, int start, int end) {
        boolean negative = false;
        int result = 0;
        if (buf[start] == '-') {
            start++;
            negative = true;
        }
        int count = end - start;
        if (count < 1) {
            return -1;
        }
        for (int i = start; i< end; i++) {
            int digit = buf[i] - 48;
            if (digit < 0 || digit > 9 ) {
                return -1;
            }
            result *= 10;
            result -= digit;
        }
        if (!negative) {
            result = -result;
        }
        return result;
    }

    public static String parseString(char[] buf, int from, int to) {
        int index = from;
        int writePosition = from;
        while (index < to) {
            if (buf[index] == '\\') {
                buf[index] = (char)parsePositiveInteger(buf, index + 2, index + 6);
                index += 6;
            } else {
                index++;
            }
            writePosition++;
        }
        return new String(buf, from, writePosition - from);
    }



}
