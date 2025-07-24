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
package com.github.jlangch.aviron.commands.scan;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.jlangch.aviron.commands.Command;
import com.github.jlangch.aviron.dto.ScanResult;
import com.github.jlangch.aviron.ex.InvalidResponseException;
import com.github.jlangch.aviron.ex.ScanFailureException;


public abstract class ScanCommand extends Command<ScanResult> {

    @Override
    protected ScanResult parseResponse(final String responseString) {
        try {
            if (RESPONSE_OK.matcher(responseString).matches()) {
                return ScanResult.ok();
            }

            if (RESPONSE_VIRUS_FOUND.matcher(responseString).find()) {
                final Map<String, List<String>> foundViruses = 
                    Arrays.stream(responseString.split("\n"))
                          .map(RESPONSE_VIRUS_FOUND_LINE::matcher)
                          .filter(Matcher::matches)
                          .map(matcher -> new AbstractMap.SimpleEntry<>(
                                                matcher.group(2),
                                                matcher.group(3)))
                          .collect(Collectors.groupingBy(
                                     Map.Entry::getKey,
                                     Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

                return ScanResult.virusFound(foundViruses);
            }

            if (RESPONSE_ERROR.matcher(responseString).matches()) {
                throw new ScanFailureException(responseString);
            }

            throw new InvalidResponseException(responseString);
        } 
        catch (IllegalStateException ex) {
            throw new InvalidResponseException(responseString);
        }
    }


    private static final Pattern RESPONSE_OK               = Pattern.compile("(.+) OK$", Pattern.UNIX_LINES);
    private static final Pattern RESPONSE_VIRUS_FOUND      = Pattern.compile("(.+) FOUND$", Pattern.MULTILINE | Pattern.UNIX_LINES);
    private static final Pattern RESPONSE_ERROR            = Pattern.compile("(.+) ERROR", Pattern.UNIX_LINES);
    private static final Pattern RESPONSE_VIRUS_FOUND_LINE = Pattern.compile("(.+: )?(.+): (.+) FOUND$", Pattern.UNIX_LINES);
}