/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.nio.file.zip.raes;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.fs.archive.zip.raes.TestZipRaesDriver;
import de.schlichtherle.truezip.key.MockView;
import static de.schlichtherle.truezip.key.MockView.Action.CANCEL;
import static de.schlichtherle.truezip.key.MockView.Action.ENTER;
import de.schlichtherle.truezip.nio.file.TPath;
import de.schlichtherle.truezip.nio.file.TPathITSuite;
import java.io.IOException;
import static java.nio.file.Files.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class ZipRaesPathIT extends TPathITSuite<TestZipRaesDriver> {

    private MockView<AesCipherParameters> view;

    @Override
    protected String getSuffixList() {
        return "tzp";
    }

    @Override
    protected TestZipRaesDriver newArchiveDriver() {
        final TestZipRaesDriver
                driver = new TestZipRaesDriver(getTestConfig().getIOPoolProvider());
        view = driver.getView();
        return driver;
    }

    @Test
    public void testCancelling() throws IOException {
        view.setAction(CANCEL);

        final TPath archive = getArchive();
        assertFalse(exists(archive.toNonArchivePath()));

        final TPath entry1 = archive.resolve("entry1");
        try {
            createDirectory(entry1);
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        try {
            createFile(entry1);
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }

        final TPath entry2 = archive.resolve("entry2");
        try {
            createDirectory(entry2);
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        try {
            createFile(entry2);
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
    }

    @Test
    public void testFileStatus() throws IOException {
        final TPath archive = getArchive();
        final TPath inner = archive.resolve("inner" + getSuffix());

        createDirectory(archive);
        createDirectory(inner);

        umount();
        view.setAction(CANCEL);
        assertTrue(exists(archive));
        assertFalse(isDirectory(archive));
        assertFalse(isRegularFile(archive));

        umount();
        view.setAction(ENTER);
        assertTrue(exists(archive));
        assertTrue(isDirectory(archive));
        assertFalse(isRegularFile(archive));

        view.setAction(CANCEL);
        assertTrue(exists(inner));
        assertFalse(isDirectory(inner));
        assertFalse(isRegularFile(inner));

        umount();
        try {
            archive.toFile().rm_r();
            fail();
        } catch (IOException expected) {
        }

        umount();
        view.setAction(ENTER);
        archive.toFile().rm_r();
    }

    /**
     * Skipped because appending to a RAES encrypted ZIP file is not possible
     * by design.
     * 
     * @deprecated 
     */
    @Deprecated
    @Override
    public void testGrowing() {
    }
}