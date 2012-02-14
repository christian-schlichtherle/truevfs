/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.nio.file;

import de.schlichtherle.truezip.file.MockArchiveDriverTestBase;
import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsPath;
import static de.schlichtherle.truezip.nio.file.TPathScanner.parent;
import static java.io.File.separatorChar;
import java.net.URI;
import java.net.URISyntaxException;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TPathScannerTest extends MockArchiveDriverTestBase {

    @Test
    public void testScan() throws URISyntaxException {
        if ('\\' == separatorChar) {
            for (final String[] params : new String[][] {
                // $parent, $member, $path, [$mountPoint]
                { "file:/", "//foo/bar", "file://foo/bar", "file://foo/" },
                { "file:/", "//foo/bar/", "file://foo/bar/", "file://foo/bar/" },
                { "file:/", "c%3A/foo", "file:/c:/foo", "file:/c:/" },
                { "file:/", "c%3A/foo", "file:/c:/foo", "file:/c:/" },
                { "file:///c:/", "//foo/bar", "file://foo/bar", "file://foo/" },
                { "file:///c:/", "//foo/bar/", "file://foo/bar/", "file://foo/bar/" },
                { "file:///c:/", "c%3A/foo", "file:/c:/foo", "file:/c:/" },
                { "file:///c:/", "c%3A/foo", "file:/c:/foo", "file:/c:/" },
                { "file://host/share/", "//foo/bar", "file://foo/bar", "file://foo/" },
                { "file://host/share/", "//foo/bar/", "file://foo/bar/", "file://foo/bar/" },
                { "file://host/share/", "c%3A/foo", "file:/c:/foo", "file:/c:/" },
                { "file://host/share/", "c%3A/foo", "file:/c:/foo", "file:/c:/" },
            }) {
                assertScan(params);
            }
        }
        for (final String[] params : new String[][] {
            // $parent, $member, $path, [$mountPoint]
            { "foo", "bar", "foo/bar", null },
            { "foo", "..", "", null },
            { "foo/bar", "../..", "", null },
            { "scheme:/foo", "..", "scheme:/", "scheme:/" },
            { "scheme:/foo/bar", "", "scheme:/foo/bar", "scheme:/foo/" },
            { "scheme:/foo/bar", "..", "scheme:/foo/", "scheme:/foo/" },
            { "scheme:/foo/bar", "../..", "scheme:/", "scheme:/" },
            { "scheme:/foo.mok/bar.mok", "../..", "scheme:/", "scheme:/" },
            { "mok:mok:scheme:/foo.mok!/bar.mok!/", "", "mok:mok:scheme:/foo.mok!/bar.mok!/", "mok:mok:scheme:/foo.mok!/bar.mok!/" },
            { "mok:mok:scheme:/foo.mok!/bar.mok!/", "..", "mok:scheme:/foo.mok!/", "mok:scheme:/foo.mok!/" },
            { "mok:mok:scheme:/foo.mok!/bar.mok!/", "../..", "scheme:/", "scheme:/" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "", "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "mok:mok:scheme:/foo.mok!/x/bar.mok!/" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "..", "mok:mok:scheme:/foo.mok!/x/bar.mok!/", "mok:mok:scheme:/foo.mok!/x/bar.mok!/" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../..", "mok:scheme:/foo.mok!/x", "mok:scheme:/foo.mok!/" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../../..", "mok:scheme:/foo.mok!/", "mok:scheme:/foo.mok!/" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../../../..", "scheme:/", "scheme:/" },
            //{ "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../../../../..", "null" },
        }) {
            assertScan(params);
        }
    }

    private static void assertScan(final String... params) throws URISyntaxException {
        final FsPath parent = new FsPath(new URI(params[0]));
        final URI member = new URI(params[1]);
        final FsPath path = new FsPath(new URI(params[2]));
        final FsMountPoint mountPoint = null == params[3]
                ? null
                : new FsMountPoint(new URI(params[3]));
        final FsPath result = new TPathScanner(
                    TConfig.get().getArchiveDetector())
                .scan(parent, member);
        assertThat(result, equalTo(path));
        assertThat(result.getMountPoint(), is(mountPoint));
    }

    @Test
    public void testParent() throws URISyntaxException {
        for (String[] params : new String[][] {
            // $path, $parent
            { "", null },
            { "foo", "" },
            { "file:/", null },
            { "file:/foo", "file:/" },
            { "file:/foo/", "file:/" },
            { "file:/foo/bar", "file:/foo/" },
            { "file:/foo/bar/", "file:/foo/" },
            { "mok:file:/foo!/", "file:/" },
            { "mok:file:/foo!/bar", "mok:file:/foo!/" },
            { "mok:mok:file:/foo!/bar!/", "mok:file:/foo!/" },
            { "mok:mok:file:/foo!/bar!/baz", "mok:mok:file:/foo!/bar!/" },
            { "mok:mok:file:/foo!/bar!/baz/boom", "mok:mok:file:/foo!/bar!/baz" },
        }) {
            final FsPath path = new FsPath(new URI(params[0]));
            final FsPath parent = params[1] == null
                    ? null
                    : new FsPath(new URI(params[1]));
            assertThat(parent(path), is(parent));
        }
    }
}
