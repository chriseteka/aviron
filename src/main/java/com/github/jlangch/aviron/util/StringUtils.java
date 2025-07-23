/*                 _                 
 *       /\       (_)            
 *      /  \__   ___ _ __ ___  _ __  
 *     / /\ \ \ / / | '__/ _ \| '_ \ 
 *    / ____ \ V /| | | | (_) | | | |
 *   /_/    \_\_/ |_|_|  \___/|_| |_|
 *
 *
 * Copyright 2025 Aviron
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    public static boolean isNotBlank(final String s) {
        return !isBlank(s);
    }

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

    public static String stripEnd(final String str, final String tail) {
        if (isBlank(str) || isBlank(tail)) {
            return str;
        }

        if (str.equals(tail)) {
            return "";
        }
        else if (str.endsWith(tail)) {
            return str.substring(0, str.length() - tail.length());
        }
        else {
            return str;
        }
    }

    public static List<String> toList(final String... items) {
        final List<String> list = new ArrayList<>();
        
        for(String it : items) {
            list.add(it);
        }
        return list;
    }

}
