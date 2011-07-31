/*
 * Copyright 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.nio.file;

import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.spi.FsDriverService;
import de.schlichtherle.truezip.util.SuffixSet;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class FileDriverServiceTest {

    public static final String DRIVER_LIST = "file";

    private FsDriverService instance;

    @Before
    public void setUp() {
        instance = new FileDriverService();
    }

    @Test
    public void testGet() {
        for (String scheme : new SuffixSet(DRIVER_LIST))
            assertThat(instance.get().get(FsScheme.create(scheme)), notNullValue());
    }

    @Test
    public void testImmutability() {
        try {
            instance.get().remove(FsScheme.create("file"));
            fail("put");
        } catch (UnsupportedOperationException ex) {
        }
    }
}
