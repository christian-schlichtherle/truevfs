/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.file;

import de.truezip.driver.zip.TestWinZipAesDriver;
import de.truezip.file.TFile;
import de.truezip.file.TFileITSuite;
import de.truezip.key.impl.MockView;
import static de.truezip.key.impl.MockView.Action.CANCEL;
import static de.truezip.key.impl.MockView.Action.ENTER;
import de.truezip.key.param.AesPbeParameters;
import java.io.IOException;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public final class WinZipAesFileIT extends TFileITSuite<TestWinZipAesDriver> {

    private MockView<AesPbeParameters> view;

    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected TestWinZipAesDriver newArchiveDriver() {
        final TestWinZipAesDriver driver = new TestWinZipAesDriver(
                getTestConfig().getIOPoolProvider());
        view = driver.getView();
        return driver;
    }

    @Test
    public void testCancelling() throws IOException {
        view.setAction(CANCEL);

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
        view.setAction(CANCEL);
        assertTrue(archive.exists());
        assertTrue(archive.isDirectory());
        assertFalse(archive.isFile());

        umount();
        view.setAction(ENTER);
        assertTrue(archive.exists());
        assertTrue(archive.isDirectory());
        assertFalse(archive.isFile());

        view.setAction(CANCEL);
        assertTrue(inner.exists());
        assertTrue(inner.isDirectory());
        assertFalse(inner.isFile());

        umount();
        archive.rm_r();
    }
}