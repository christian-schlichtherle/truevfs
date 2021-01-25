/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import java.net.URI;
import java.net.URISyntaxException;
import net.java.truevfs.kernel.spec.FsMountPoint;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class TVFSTest extends MockArchiveDriverTestBase {

    @Test
    public void testMountPoint() throws URISyntaxException {
        for (final String[] params : new String[][] {
            // { $file, $mountPoint }
            { "fö ö.mok/b är", CURRENT_DIRECTORY + "fö%20ö.mok/b%20är/" },
            { "fö ö.mok", "mok:" + CURRENT_DIRECTORY + "fö%20ö.mok!/" },
            { "fö ö", CURRENT_DIRECTORY + "fö%20ö/" },
            { ".", CURRENT_DIRECTORY.toString() },
            { "", CURRENT_DIRECTORY.toString() },
        }) {
            final TFile file = new TFile(params[0]);
            final FsMountPoint mountPoint = new FsMountPoint(new URI(params[1]));
            assertThat(TVFS.mountPoint(file), is(mountPoint));
        }
    }
}
