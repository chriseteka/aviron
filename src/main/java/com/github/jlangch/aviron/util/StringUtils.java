package com.github.jlangch.aviron.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


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

    public static boolean isBlank(final String s) {
        return s == null ? true : s.trim().isEmpty();
    }

	/**
	 * Splits a text into lines
	 * 
	 * @param text	a string
	 * 
	 * @return the lines (maybe empty if the text was <tt>null</tt> or empty
	 */
	public static List<String> splitIntoLines(final String text) {
		if (text == null || text.isEmpty()) {
			return new ArrayList<>();
		}
		else {
			try(final BufferedReader br = new BufferedReader(new StringReader(text))) {
				return br.lines().collect(Collectors.toList());
			}
			catch(IOException | RuntimeException ex) {
				throw new RuntimeException("Failed to split text into lines", ex);
			}
		}
	}

}
