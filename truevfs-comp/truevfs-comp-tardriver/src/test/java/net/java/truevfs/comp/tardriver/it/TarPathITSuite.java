/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.tardriver.it;

import java.io.IOException;
import net.java.truevfs.access.TPath;
import net.java.truevfs.access.it.TPathITSuite;
import net.java.truevfs.kernel.spec.FsArchiveDriver;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @param   <D> The type of the archive driver.
 * @author  Christian Schlichtherle
 */
public abstract class TarPathITSuite<D extends FsArchiveDriver<?>>
extends TPathITSuite<D> {

    private static final int LONG_PATH_NAME_LENGTH = 67;

    private static String longPathName() {
        final StringBuilder sb = new StringBuilder(LONG_PATH_NAME_LENGTH);
        for (int i = 0; i < LONG_PATH_NAME_LENGTH; i++) sb.append(i % 10);
        return sb.toString();
    }

    /** Test for http://java.net/jira/browse/TRUEZIP-286 . */
    @Test
    public void testLongPathName() throws IOException {
        final TPath entry = new TPath(getArchive()).resolve(longPathName());
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
