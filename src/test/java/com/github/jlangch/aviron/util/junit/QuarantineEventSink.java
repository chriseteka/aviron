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
package com.github.jlangch.aviron.util.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.jlangch.aviron.events.QuarantineEvent;


/**
 * An Event sink to support unit tests
 */
public class QuarantineEventSink {

    public QuarantineEventSink() {
    }

    public void process(final QuarantineEvent event) {
        events.add(event);
    }

    public void clear() {
        events.clear();
    }

    public int size() {
        return events.size();
    }

    public List<QuarantineEvent> events() {
        return Collections.unmodifiableList(events);
    }


    private List<QuarantineEvent> events = new ArrayList<>();
}
