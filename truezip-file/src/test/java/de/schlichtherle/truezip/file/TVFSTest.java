/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file;

import static de.schlichtherle.truezip.file.TVFS.mountPoint;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsSyncOption;
import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import static de.schlichtherle.truezip.fs.FsSyncOptions.SYNC;
import static de.schlichtherle.truezip.fs.FsSyncOptions.UMOUNT;
import de.schlichtherle.truezip.util.BitField;
import java.net.URI;
import java.net.URISyntaxException;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class TVFSTest extends MockArchiveDriverTestBase {

    @Test
    public void testMountPoint() throws URISyntaxException {
        for (final String[] params : new String[][] {
            // { $file, $mountPoint }
            { "foo.mok/bar", CURRENT_DIRECTORY + "foo.mok/bar/" },
            { "foo.mok", "mok:" + CURRENT_DIRECTORY + "foo.mok!/" },
            { "foo", CURRENT_DIRECTORY + "foo/" },
            { ".", CURRENT_DIRECTORY.toString() },
            { "", CURRENT_DIRECTORY.toString() },
        }) {
            final TFile file = new TFile(params[0]);
            final FsMountPoint mountPoint = new FsMountPoint(new URI(params[1]));
            assertThat(mountPoint(file), is(mountPoint));
        }
    }
}
