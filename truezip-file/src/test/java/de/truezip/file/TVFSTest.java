/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.file;

import static de.truezip.file.TVFS.mountPoint;
import de.truezip.kernel.FsMountPoint;
import java.net.URI;
import java.net.URISyntaxException;
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
            assertThat(mountPoint(file), is(mountPoint));
        }
    }
}
