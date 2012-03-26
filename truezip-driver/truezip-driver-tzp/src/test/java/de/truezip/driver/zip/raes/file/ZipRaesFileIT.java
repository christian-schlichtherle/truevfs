/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.file;

import de.truezip.kernel.key.param.AesPbeParameters;
import de.truezip.file.TFile;
import de.truezip.file.TFileITSuite;
import de.truezip.driver.zip.raes.TestZipRaesDriver;
import de.truezip.kernel.key.impl.MockView;
import static de.truezip.kernel.key.impl.MockView.Action.CANCEL;
import static de.truezip.kernel.key.impl.MockView.Action.ENTER;
import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class ZipRaesFileIT extends TFileITSuite<TestZipRaesDriver> {

    private MockView<AesPbeParameters> view;

    @Override
    protected String getSuffixList() {
        return "tzp";
    }

    @Override
    protected TestZipRaesDriver newArchiveDriver() {
        final TestZipRaesDriver driver = new TestZipRaesDriver(
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
        final TFile inner = new TFile(archive, "inner" + getSuffix());

        assertTrue(archive.mkdir());
        assertTrue(inner.mkdir());

        umount();
        view.setAction(CANCEL);
        assertTrue(archive.exists());
        assertFalse(archive.isDirectory());
        assertFalse(archive.isFile());

        umount();
        view.setAction(ENTER);
        assertTrue(archive.exists());
        assertTrue(archive.isDirectory());
        assertFalse(archive.isFile());

        view.setAction(CANCEL);
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
        view.setAction(ENTER);
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
