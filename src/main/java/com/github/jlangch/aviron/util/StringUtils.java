package com.github.jlangch.aviron.util;


public class StringUtils {

    public static String trimToNull(final String s) {
        if (s == null) {
            return s;
        }
        final String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static String trimToEmpty(final String s) {
        return s == null ? "" : s.trim();
    }

}
