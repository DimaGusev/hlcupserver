package com.dgusev.hl.server.util;

/**
 * Created by dgusev on 06.10.2017.
 */
public class CharArrayUtil {

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

}
