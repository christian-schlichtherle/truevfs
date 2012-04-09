/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.path;

import de.truezip.file.MockArchiveDriverTestBase;
import de.truezip.kernel.FsMountPoint;
import static de.truezip.kernel.FsUriModifier.CANONICALIZE;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public class TFileSystemProviderTest extends MockArchiveDriverTestBase {

    private TFileSystemProvider provider;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        try {
            provider = TFileSystemProvider.class.newInstance();
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void testNewFileSystemFromPath() {
        for (final Object[] params : new Object[][] {
            // $first, $more, $mountPoint
            { "foo.mok", new String[] { "x", "bar.mok", "y" }, "mok:mok:" + CURRENT_DIRECTORY + "foo.mok!/x/bar.mok!/" },
            { "foo.mok", new String[] { "bar.mok" }, "mok:mok:" + CURRENT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "foo.mok", new String[] { "x" }, "mok:" + CURRENT_DIRECTORY + "foo.mok!/" },
            { "foo.mok", NO_STRINGS, "mok:" + CURRENT_DIRECTORY + "foo.mok!/" },
            { "foo", new String[] { "x" }, null },
            { "foo", NO_STRINGS, null },
        }) {
            final String first = params[0].toString();
            final String[] more = (String[]) params[1];
            final Path path = Paths.get(first, more);
            final FsMountPoint mountPoint = null == params[2]
                    ? null
                    : FsMountPoint.create(URI.create(params[2].toString()), CANONICALIZE);
            try {
                final TFileSystem fs = provider.newFileSystem(path, getEnvironment());
                if (null == mountPoint)
                    fail();
                assertThat(fs.getMountPoint(), is(mountPoint));
                for (final String entry : new String[] {
                    "",
                    "foo",
                    "/",
                    "/foo",
                }) {
                    assertThat(fs.getPath(entry).getFileSystem(), sameInstance(fs));
                }
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
                final TFileSystem fs = provider.newFileSystem(uri, getEnvironment());
                if (null == mountPoint)
                    fail();
                assertThat(fs.getMountPoint(), is(mountPoint));
                for (final String entry : new String[] {
                    "",
                    "foo",
                    "/",
                    "/foo",
                }) {
                    assertThat(fs.getPath(entry).getFileSystem(), sameInstance(fs));
                }
            } catch (UnsupportedOperationException ex) {
                if (null != mountPoint)
                    throw ex;
            }
        }
    }

    @Test
    public void testGetPath() {
        for (final Object[] params : new Object[][] {
            // $uri, $succeeds
            { "", false },
            { "/", false },
            { "file:/", false },
            { "tpath:/", true },
        }) {
            final URI uri = URI.create(params[0].toString());
            final boolean succeeds = (Boolean) params[1];
            TPath path;
            try {
                path = provider.getPath(uri);
                if (!succeeds)
                    fail();
            } catch (IllegalArgumentException ex) {
                if (succeeds)
                    throw ex;
                return;
            }
            assertThat(path.getFileSystem().provider(), sameInstance(provider));
            assertThat(path.toUri().getScheme(), is(provider.getScheme()));
        }
    }
}