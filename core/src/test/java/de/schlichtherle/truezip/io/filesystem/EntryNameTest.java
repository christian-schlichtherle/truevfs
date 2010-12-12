/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.filesystem;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class EntryNameTest {

    @Test
    public void testConstructorWithValidUri() throws URISyntaxException {
        // This is by no means a complete test, but all the other constructors
        // are already tested by MountPointTest and PathTest.
        for (final String[] params : new String[][] {
            { "foo", "", "foo" },
            { "foo/", "", "foo/" },
            { "", "foo", "foo" },
            { "", "foo/", "foo/" },
            { "foo", "bar", "foo/bar" },
            { "foo", "bar/", "foo/bar/" },
            { "foo/", "bar", "foo/bar" },
            { "foo/", "bar/", "foo/bar/" },
        }) {
            final EntryName parent = new EntryName(URI.create(params[0]));
            final EntryName member = new EntryName(URI.create(params[1]));
            final EntryName result = new EntryName(parent, member);
            assertThat(result.getUri(), equalTo(URI.create(params[2])));
        }
    }
}
