/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.archive.FsArchiveEntry;
import de.schlichtherle.truezip.zip.ZipEntry;
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
