/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.nio.fsp;

import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import org.junit.Before;
import de.schlichtherle.truezip.file.TArchiveDetector;
import java.io.File;
import java.net.URI;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TPathTest {

    @Before
    public void setUp() {
        TPath.setDefaultArchiveDetector(
                new TArchiveDetector("mok", new MockArchiveDriver()));
    }
    @After
    public void tearDown() {
        TPath.setDefaultArchiveDetector(TArchiveDetector.ALL);
    }

    @Test
    public void testInvalidUriConstructor() {
        for (String param : new String[] {
            // $uri,
            null,
            "foo:..",
            "foo:/..",
            "foo:bar:/boom!/../bang",
        }) {
            URI uri = param == null ? null : URI.create(param);
            try {
                new TPath(uri);
                fail();
            } catch (RuntimeException expected) {
            }
        }
    }

    @Test
    public void testParsingConstructor() {
        for (String[] params : new String[][] {
            // { $uri, $innerArchive, $innerEntryName, $enclArchive, $enclEntryName },
            { "foo", null, null, null, null },
            { "/foo", null, null, null, null },
            { "foo:bar:/boom!/", "foo:bar:/boom!/", "", null, null },
            { "foo:bar:/boom!/bang", "foo:bar:/boom!/", "bang", "foo:bar:/boom!/", "bang" },
            { "foo:bar:baz:/boom!/bang!/", "foo:bar:baz:/boom!/bang!/", "", "bar:baz:/boom!/", "bang" },
            { "foo:bar:baz:/boom!/bang!/peng", "foo:bar:baz:/boom!/bang!/", "peng", "foo:bar:baz:/boom!/bang!/", "peng", },
            { "foo:bar:baz:/a/../boom!/b/../bang!/c/../peng", "foo:bar:baz:/boom!/bang!/", "peng", "foo:bar:baz:/boom!/bang!/", "peng", },
            { "foo:bar:baz:/ä/../bööm!/b/../bäng!/c/../päng", "foo:bar:baz:/bööm!/bäng!/", "päng", "foo:bar:baz:/bööm!/bäng!/", "päng", },
        }) {
            URI uri = URI.create(params[0]);
            TPath path = new TPath(uri);
            TPath innerArchive = params[1] == null ? null : new TPath(URI.create(params[1]));
            TPath enclArchive = params[3] == null ? null : new TPath(URI.create(params[3]));
            assertThat(path.toUri(), equalTo(uri));
            assertThat(path.getInnerArchive(), equalTo(innerArchive));
            assertThat(path.getInnerEntryName(), equalTo(params[2]));
            assertThat(path.getEnclArchive(), equalTo(enclArchive));
            assertThat(path.getEnclEntryName(), equalTo(params[4]));
        }
    }

    @Test
    public void testScanningConstructor() {
        final String root = new File("/").toURI().toString();
        final String cd = new File("").toURI().toString();
        for (Object[] params : new Object[][] {
            // { $expectedFsPath, $expectedUri, $parent, $first, $more },
            { cd + "foo", "foo", null, "foo", new String[0] },
            { cd + "foo/bar", "foo/bar", null, "foo", new String[] { "bar" } },
            { cd + "foo/bar", "foo/bar", null, "foo", new String[] { "/bar" } },
            { cd + "foo/bar/baz", "foo/bar/baz", null, "foo", new String[] { "bar", "baz" } },
            { cd + "foo/bar/baz", "foo/bar/baz", null, "foo", new String[] { "/bar", "/baz" } },
            { root + "foo", "/foo", null, "/foo", new String[0] },
            { root + "foo/bar", "/foo/bar", null, "/foo", new String[] { "bar" } },
            { root + "foo/bar", "/foo/bar", null, "/foo", new String[] { "/bar" } },
            { root + "foo/bar/baz", "/foo/bar/baz", null, "/foo", new String[] { "bar", "baz" } },
            { root + "foo/bar/baz", "/foo/bar/baz", null, "/foo", new String[] { "/bar", "/baz" } },
            { "mok:" + cd + "archive.mok!/", "archive.mok", null, "archive.mok", new String[0] },
            { "mok:" + root + "archive.mok!/", "/archive.mok", null, "/archive.mok", new String[0] },
        }) {
            URI expectedFsPath = URI.create(params[0].toString());
            URI expectedUri = URI.create(params[1].toString());
            TPath parent = params[2] == null ? null : new TPath(URI.create(params[2].toString()));
            String first = params[3].toString();
            String[] more = (String[]) params[4];
            TPath result = new TPath(parent, first, more);
            assertThat(result.toUri(), equalTo(expectedUri));
            assertThat(result.toFsPath().toUri(), equalTo(expectedFsPath));
        }
    }
}
