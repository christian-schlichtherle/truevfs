/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.raes.access;

import net.truevfs.driver.zip.raes.TestZipRaesDriver;
import net.truevfs.access.TFile;
import net.truevfs.access.TFileITSuite;
import net.truevfs.key.MockView.Action;
import static net.truevfs.key.MockView.Action.CANCEL;
import static net.truevfs.key.MockView.Action.ENTER;
import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class ZipRaesFileIT extends TFileITSuite<TestZipRaesDriver> {

    @Override
    protected String getExtensionList() {
        return "tzp";
    }

    @Override
    protected TestZipRaesDriver newArchiveDriver() {
        return new TestZipRaesDriver(getTestConfig().getIOPoolProvider());
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
        assertFalse(entry1.mkdirs());
        try {
            assertFalse(entry1.createNewFile());
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }

        final TFile entry2 = new TFile(archive, "entry2");
        assertFalse(entry2.mkdirs());
        try {
            assertFalse(entry2.createNewFile());
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
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
        assertFalse(archive.isDirectory());
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

    /**
     * Skipped because appending to RAES encrypted ZIP files is not possible
     * by design.
     */
    @Override
    public void testGrowing() {
    }
}
