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
package de.schlichtherle.truezip.nio.file;

import de.schlichtherle.truezip.fs.FsMountPoint;
import static de.schlichtherle.truezip.fs.FsUriModifier.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TFileSystemProviderTest extends TestBase {

    private TFileSystemProvider provider;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        provider = TFileSystemProvider.class.newInstance();
    }

    @Test
    public void testNewFileSystemFromPath() {
        for (final Object[] params : new Object[][] {
            // $first, $more, $mountPoint
            { "foo.mok", new String[] { "x", "bar.mok", "y" }, "mok:mok:" + CURRENT_DIRECTORY + "foo.mok!/x/bar.mok!/" },
            { "foo.mok", new String[] { "bar.mok" }, "mok:mok:" + CURRENT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "foo.mok", new String[] { "x" }, "mok:" + CURRENT_DIRECTORY + "foo.mok!/" },
            { "foo.mok", NO_MORE, "mok:" + CURRENT_DIRECTORY + "foo.mok!/" },
            { "foo", new String[] { "x" }, null },
            { "foo", NO_MORE, null },
        }) {
            final String first = params[0].toString();
            final String[] more = (String[]) params[1];
            final Path path = Paths.get(first, more);
            final FsMountPoint mountPoint = null == params[2]
                    ? null
                    : FsMountPoint.create(URI.create(params[2].toString()), CANONICALIZE);
            try {
                final TFileSystem fs = provider.newFileSystem(path, environment);
                if (null == mountPoint)
                    fail();
                assertThat(fs.getMountPoint(), equalTo(mountPoint));
            } catch (UnsupportedOperationException ex) {
                if (null != mountPoint)
                    throw ex;
            }
        }
    }

    @Test
    public void testNewFileSystemFromUri() {
        for (final String[] params : new String[][] {
            // $uri, $mountPoint
            { provider.getScheme() + ":/foo.mok/x/bar.mok/y/", "mok:mok:" + ROOT_DIRECTORY + "foo.mok!/x/bar.mok!/" },
            { provider.getScheme() + ":/foo.mok/x/bar.mok/y", "mok:mok:" + ROOT_DIRECTORY + "foo.mok!/x/bar.mok!/" },
            { provider.getScheme() + ":/foo.mok/bar.mok/", "mok:mok:" + ROOT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { provider.getScheme() + ":/foo.mok/bar.mok", "mok:mok:" + ROOT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { provider.getScheme() + ":/foo.mok/x/", "mok:" + ROOT_DIRECTORY + "foo.mok!/" },
            { provider.getScheme() + ":/foo.mok/x", "mok:" + ROOT_DIRECTORY + "foo.mok!/" },
            { provider.getScheme() + ":/foo.mok/", "mok:" + ROOT_DIRECTORY + "foo.mok!/" },
            { provider.getScheme() + ":/foo.mok", "mok:" + ROOT_DIRECTORY + "foo.mok!/" },
            { provider.getScheme() + ":/foo/bar/x/", ROOT_DIRECTORY + "foo/bar/" },
            { provider.getScheme() + ":/foo/bar/x", ROOT_DIRECTORY + "foo/bar/" },
            { provider.getScheme() + ":/foo/x/", ROOT_DIRECTORY + "foo/" },
            { provider.getScheme() + ":/foo/x", ROOT_DIRECTORY + "foo/" },
            { provider.getScheme() + ":/x/", ROOT_DIRECTORY.toString() },
            { provider.getScheme() + ":/x", ROOT_DIRECTORY.toString() },
            { provider.getScheme() + ":/", ROOT_DIRECTORY.toString() },
        }) {
            final URI uri = URI.create(params[0]);
            final FsMountPoint mountPoint = FsMountPoint.create(URI.create(params[1]));
            try {
                final TFileSystem fs = provider.newFileSystem(uri, environment);
                if (null == mountPoint)
                    fail();
                assertThat(fs.getMountPoint(), equalTo(mountPoint));
            } catch (UnsupportedOperationException ex) {
                if (null != mountPoint)
                    throw ex;
            }
        }
    }

    @Test
    public void testGetPath() {
        for (final String[] params : new String[][] {
            // $uri
            //{ "foo" },
            //{ "bar" },
            { "tpath:/foo" },
            { "tpath:/bar" },
            //{ "cheating:/foo" }, // gets mapped to the provider's scheme.
            //{ "cheating:/bar" },
            //{ "yes-this-works-as-excepted:/c:/Users/christian/" },
        }) {
            URI uri = URI.create(params[0]);
            TPath path = provider.getPath(uri);
            assertThat(path.getFileSystem().provider(), sameInstance(provider));
        }
    }
}
