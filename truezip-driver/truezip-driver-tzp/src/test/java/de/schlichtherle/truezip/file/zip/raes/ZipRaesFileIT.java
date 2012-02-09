/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.zip.raes;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileTestSuite;
import de.schlichtherle.truezip.fs.archive.zip.raes.TestZipRaesDriver;
import de.schlichtherle.truezip.key.MockView;
import static de.schlichtherle.truezip.key.MockView.Action.CANCEL;
import static de.schlichtherle.truezip.key.MockView.Action.ENTER;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class ZipRaesFileIT extends TFileTestSuite<TestZipRaesDriver> {

    private MockView<AesCipherParameters> view;

    @Override
    protected String getSuffixList() {
        return "tzp";
    }

    @Override
    protected TestZipRaesDriver newArchiveDriver(final IOPoolProvider provider) {
        final TestZipRaesDriver driver = new TestZipRaesDriver(provider);
        view = driver.getView();
        return driver;
    }

    @Test
    public void testCancelling() throws IOException {
        view.setAction(CANCEL);

        final TFile archive = getArchive();
        assertFalse(newNonArchiveFile(archive).exists());

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

        TFile.umount();
        view.setAction(CANCEL);
        assertTrue(archive.exists());
        assertFalse(archive.isDirectory());
        assertFalse(archive.isFile());

        TFile.umount();
        view.setAction(ENTER);
        assertTrue(archive.exists());
        assertTrue(archive.isDirectory());
        assertFalse(archive.isFile());

        view.setAction(CANCEL);
        assertTrue(inner.exists());
        assertFalse(inner.isDirectory());
        assertFalse(inner.isFile());

        TFile.umount();
        try {
            archive.rm_r();
            fail();
        } catch (IOException expected) {
        }
            
        TFile.umount();
        view.setAction(ENTER);
        archive.rm_r();
    }

    /**
     * Skipped because appending to RAES encrypted ZIP files is not possible
     * by design.
     * 
     * @deprecated 
     */
    @Deprecated
    @Override
    public void testGrowing() {
    }
}
