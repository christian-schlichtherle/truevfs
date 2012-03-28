/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.file;

import de.truezip.driver.zip.TestWinZipAesDriver;
import de.truezip.file.TFile;
import de.truezip.file.TFileITSuite;
import static de.truezip.key.MockView.Action.CANCEL;
import static de.truezip.key.MockView.Action.ENTER;
import java.io.IOException;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public final class WinZipAesFileIT extends TFileITSuite<TestWinZipAesDriver> {

    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected TestWinZipAesDriver newArchiveDriver() {
        return new TestWinZipAesDriver(getTestConfig().getIOPoolProvider());
    }

    @Test
    public void testCancelling() throws IOException {
        getArchiveDriver().getView().setAction(CANCEL);

        final TFile archive = getArchive();
        assertFalse(archive.toNonArchiveFile().exists());

        final TFile entry1 = new TFile(archive, "entry1");
        assertTrue(entry1.mkdirs());
        entry1.rm();
        assertTrue(entry1.createNewFile());
        entry1.rm();

        final TFile entry2 = new TFile(archive, "entry2");
        assertTrue(entry2.mkdirs());
        entry2.rm();
        assertTrue(entry2.createNewFile());
        entry2.rm();
    }

    @Test
    public void testFileStatus() throws IOException {
        final TFile archive = getArchive();
        final TFile inner = new TFile(archive, "inner" + getSuffix());

        assertTrue(archive.mkdir());
        assertTrue(inner.mkdir());

        umount();
        getArchiveDriver().getView().setAction(CANCEL);
        assertTrue(archive.exists());
        assertTrue(archive.isDirectory());
        assertFalse(archive.isFile());

        umount();
        getArchiveDriver().getView().setAction(ENTER);
        assertTrue(archive.exists());
        assertTrue(archive.isDirectory());
        assertFalse(archive.isFile());

        getArchiveDriver().getView().setAction(CANCEL);
        assertTrue(inner.exists());
        assertTrue(inner.isDirectory());
        assertFalse(inner.isFile());

        umount();
        archive.rm_r();
    }
}