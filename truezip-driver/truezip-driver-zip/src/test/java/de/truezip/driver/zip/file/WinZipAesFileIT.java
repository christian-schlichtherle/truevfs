/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.file;

import de.truezip.driver.zip.TestWinZipAesDriver;
import de.truezip.file.TConfig;
import de.truezip.file.TFile;
import de.truezip.file.TFileITSuite;
import static de.truezip.kernel.FsAccessOption.ENCRYPT;
import de.truezip.key.MockView.Action;
import static de.truezip.key.MockView.Action.CANCEL;
import static de.truezip.key.MockView.Action.ENTER;
import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class WinZipAesFileIT extends TFileITSuite<TestWinZipAesDriver> {

    @Override
    protected String getExtensionList() {
        return "zip";
    }

    @Override
    protected TestWinZipAesDriver newArchiveDriver() {
        return new TestWinZipAesDriver(getTestConfig().getIOPoolProvider());
    }

    @Override
    public void setUp() throws IOException {
        super.setUp();
        final TConfig config = TConfig.get();
        config.setAccessPreferences(config.getAccessPreferences().set(ENCRYPT));
    }

    private void setAction(Action action) {
        getArchiveDriver().getView().setAction(action);
    }

    @Test
    public void testCancelling() throws IOException {
        setAction(CANCEL);

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
        final TFile inner = new TFile(archive, "inner" + getExtension());

        assertTrue(archive.mkdir());
        assertTrue(inner.mkdir());

        umount();
        setAction(CANCEL);
        assertTrue(archive.exists());
        assertTrue(archive.isDirectory());
        assertFalse(archive.isFile());

        umount();
        setAction(ENTER);
        assertTrue(archive.exists());
        assertTrue(archive.isDirectory());
        assertFalse(archive.isFile());

        setAction(CANCEL);
        assertTrue(inner.exists());
        assertFalse(inner.isDirectory());
        assertFalse(inner.isFile());

        umount();
        try {
            archive.rm_r();
            fail();
        } catch (IOException expected) {
        }

        umount();
        setAction(ENTER);
        archive.rm_r();
    }
}
