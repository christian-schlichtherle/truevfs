/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.access;

import net.truevfs.access.MockArchiveDriverTestBase;
import net.truevfs.kernel.FsMountPoint;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public class TFileSystemTest extends MockArchiveDriverTestBase {

    @Test
    public void testNewFileSystem() throws IOException {
        for (String[] params : new String[][] {
            // $uri, $mountPoint
            { "tpath:/", "file:/" },
            { "tpath:///", "file:/" },
            { "tpath:/foo", "file:/" },
            { "tpath:/foo/", "file:/" },
            { "tpath:/foo/bar", "file:/foo/" },
            { "tpath:/foo/bar/", "file:/foo/" },
            { "tpath:/foo/bar.mok/", "mok:file:/foo/bar.mok!/" },
            { "tpath:/foo.mok/bar", "mok:file:/foo.mok!/" },
            { "tpath:/foo.mok/bar.mok", "mok:mok:file:/foo.mok!/bar.mok!/" },
        }) {
            final URI uri = URI.create(params[0]);
            final FsMountPoint mountPoint = FsMountPoint.create(
                    URI.create(params[1]));
            final TFileSystem fs = (TFileSystem) FileSystems.newFileSystem(
                    uri, getEnvironment(), TFileSystemTest.class.getClassLoader());
            fs.close();
            assertThat(fs.isOpen(), is(true));
            assertThat(fs.getMountPoint(), is(mountPoint));
        }
    }
}