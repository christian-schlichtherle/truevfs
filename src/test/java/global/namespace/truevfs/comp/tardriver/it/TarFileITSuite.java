/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.tardriver.it;

import global.namespace.truevfs.access.TFile;
import global.namespace.truevfs.access.it.TFileITSuite;
import global.namespace.truevfs.kernel.api.FsArchiveDriver;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

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
     */
    @Ignore
    @Override
    public final void testGrowing() { }
}
