/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file.tar;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileITSuite;
import de.schlichtherle.truezip.fs.FsArchiveDriver;
import java.io.IOException;
import org.junit.Test;

/**
 * @param  <D> The type of the archive driver.
 * @author Christian Schlichtherle
 */
public abstract class TarFileITSuite<D extends FsArchiveDriver<?>>
extends TFileITSuite<D> {

    private static final int LONG_PATH_NAME_LENGTH = 67;

    private static String longPathName() {
        final StringBuilder sb = new StringBuilder(LONG_PATH_NAME_LENGTH);
        for (int i = 0; i < LONG_PATH_NAME_LENGTH; i++) sb.append(i % 10);
        return sb.toString();
    }

    /** Test for http://java.net/jira/browse/TRUEZIP-286 . */
    @Test
    public void testLongPathName() throws IOException {
        final TFile entry = new TFile(getArchive(), longPathName());
        createTestFile(entry);
        umount();
        verifyTestFile(entry);
    }

    /**
     * Skipped because appending to TAR files is currently not supported.
     * 
     * @deprecated 
     */
    @Deprecated
    @Override
    public final void testGrowing() {
    }
}
