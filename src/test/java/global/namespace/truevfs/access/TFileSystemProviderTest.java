/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access;

import global.namespace.truevfs.kernel.api.FsMountPoint;
import lombok.val;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

import static global.namespace.truevfs.kernel.api.FsUriModifier.CANONICALIZE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author  Christian Schlichtherle
 */
public final class TFileSystemProviderTest extends MockArchiveDriverTestBase {

    private TFileSystemProvider provider;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        try {
            provider = TFileSystemProvider.class.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testNewFileSystemFromPath() {
        for (val params : new Object[][] {
            // $first, $more, $mountPoint
            { "foo.mok", new String[] { "x", "bar.mok", "y" }, "mok:mok:" + CURRENT_DIRECTORY + "foo.mok!/x/bar.mok!/" },
            { "foo.mok", new String[] { "bar.mok" }, "mok:mok:" + CURRENT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "foo.mok", new String[] { "x" }, "mok:" + CURRENT_DIRECTORY + "foo.mok!/" },
            { "foo.mok", NO_STRINGS, "mok:" + CURRENT_DIRECTORY + "foo.mok!/" },
            { "foo", new String[] { "x" }, null },
            { "foo", NO_STRINGS, null },
        }) {
            val first = params[0].toString();
            val more = (String[]) params[1];
            val path = Paths.get(first, more);
            val mountPoint = null == params[2]
                    ? null
                    : FsMountPoint.create(URI.create(params[2].toString()), CANONICALIZE);
            try {
                val fs = provider.newFileSystem(path, getEnvironment());
                if (null == mountPoint) {
                    fail();
                }
                assertThat(fs.getMountPoint(), is(mountPoint));
                for (val entry : new String[] {
                    "",
                    "foo",
                    "/",
                    "/foo",
                }) {
                    assertThat(fs.getPath(entry).getFileSystem(), sameInstance(fs));
                }
            } catch (final UnsupportedOperationException e) {
                if (null != mountPoint) {
                    throw e;
                }
            }
        }
    }

    @Test
    public void testNewFileSystemFromUri() {
        for (val params : new String[][] {
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
            val uri = URI.create(params[0]);
            val mountPoint = FsMountPoint.create(URI.create(params[1]));
            val fs = provider.newFileSystem(uri, getEnvironment());
            assertThat(fs.getMountPoint(), is(mountPoint));
            for (val entry : new String[] {
                "",
                "foo",
                "/",
                "/foo",
            }) {
                assertThat(fs.getPath(entry).getFileSystem(), sameInstance(fs));
            }
        }
    }

    @Test
    public void testGetPath() {
        for (val params : new Object[][] {
            // $uri, $succeeds
            { "", false },
            { "/", false },
            { "file:/", false },
            { "tpath:/", true },
        }) {
            val uri = URI.create(params[0].toString());
            val succeeds = (Boolean) params[1];
            TPath path;
            try {
                path = provider.getPath(uri);
                if (!succeeds) {
                    fail();
                }
            } catch (final IllegalArgumentException e) {
                if (succeeds) {
                    throw e;
                }
                return;
            }
            assertThat(path.getFileSystem().provider(), sameInstance(provider));
            assertThat(path.getUri().getScheme(), is(provider.getScheme()));
        }
    }
}
