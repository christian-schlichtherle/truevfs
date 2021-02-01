/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access;

import global.namespace.truevfs.kernel.spec.FsMountPoint;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
