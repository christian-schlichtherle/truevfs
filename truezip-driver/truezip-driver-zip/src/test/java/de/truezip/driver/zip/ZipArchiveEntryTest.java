/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.kernel.fs.FsArchiveEntry;
import de.truezip.driver.zip.io.ZipEntry;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class ZipArchiveEntryTest {

    @Test
    @SuppressWarnings("all")
    public void testClassInvariants() {
        assertTrue(ZipEntry.UNKNOWN == FsArchiveEntry.UNKNOWN);
    }
}
