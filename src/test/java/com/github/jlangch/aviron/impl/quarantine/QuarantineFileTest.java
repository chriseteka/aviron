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
package com.github.jlangch.aviron.impl.quarantine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.github.jlangch.aviron.dto.QuarantineFile;
import com.github.jlangch.aviron.events.QuarantineFileAction;
import com.github.jlangch.aviron.impl.util.StringUtils;


class QuarantineFileTest {

    @Test
    void testNew() {
        final LocalDateTime ts = LocalDateTime.now();

        final QuarantineFile qf = new QuarantineFile(
                                        "test.pdf",
                                        new File("/data/test.pdf"),
                                        StringUtils.toList("v1", "v2"),
                                        QuarantineFileAction.MOVE,
                                        ts,
                                        "hash:123");

        assertEquals("test.pdf", qf.getQuarantineFileName());
        assertEquals(new File("/data/test.pdf"), qf.getInfectedFile());
        assertEquals(StringUtils.toList("v1", "v2"), qf.getVirusList());
        assertEquals(QuarantineFileAction.MOVE, qf.getAction());
        assertEquals(ts, qf.getQuarantinedAt());
        assertEquals("hash:123", qf.getHash());
    }

    @Test
    void testFormatAndParse() {
        final LocalDateTime ts = LocalDateTime.now();

        final QuarantineFile tmp = new QuarantineFile(
                                        "test.pdf",
                                        new File("/data/test.pdf"),
                                        StringUtils.toList("v1", "v2"),
                                        QuarantineFileAction.MOVE,
                                        ts,
                                        "hash:123");

        final String fmt = tmp.format();

        final QuarantineFile qf = QuarantineFile.from("test.pdf", fmt);

        assertEquals("test.pdf", qf.getQuarantineFileName());
        assertEquals(new File("/data/test.pdf"), qf.getInfectedFile());
        assertEquals(StringUtils.toList("v1", "v2"), qf.getVirusList());
        assertEquals(QuarantineFileAction.MOVE, qf.getAction());
        assertEquals(ts, qf.getQuarantinedAt());
        assertEquals("hash:123", qf.getHash());
    }

}
