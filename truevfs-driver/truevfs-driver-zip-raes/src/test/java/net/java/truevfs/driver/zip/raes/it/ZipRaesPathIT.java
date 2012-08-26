/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes.it;

import java.io.IOException;
import static java.nio.file.Files.*;
import net.java.truevfs.access.TPath;
import net.java.truevfs.access.it.TPathITSuite;
import net.java.truevfs.driver.zip.raes.TestZipRaesDriver;
import net.java.truevfs.key.spec.MockView.Action;
import static net.java.truevfs.key.spec.MockView.Action.CANCEL;
import static net.java.truevfs.key.spec.MockView.Action.ENTER;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class ZipRaesPathIT extends TPathITSuite<TestZipRaesDriver> {

    @Override
    protected String getExtensionList() {
        return "tzp";
    }

    @Override
    protected TestZipRaesDriver newArchiveDriver() {
        return new TestZipRaesDriver();
    }

    private void setAction(Action action) {
        getArchiveDriver().getView().setAction(action);
    }

    @Test
    public void testCancelling() throws IOException {
        setAction(CANCEL);

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
        final TPath inner = archive.resolve("inner" + getExtension());

        createDirectory(archive);
        createDirectory(inner);

        umount();
        setAction(CANCEL);
        assertTrue(exists(archive));
        assertFalse(isDirectory(archive));
        assertFalse(isRegularFile(archive));

        umount();
        setAction(ENTER);
        assertTrue(exists(archive));
        assertTrue(isDirectory(archive));
        assertFalse(isRegularFile(archive));

        setAction(CANCEL);
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
        setAction(ENTER);
        archive.toFile().rm_r();
    }

    /**
     * Skipped because appending to a RAES encrypted ZIP file is not possible
     * by design.
     */
    @Override
    public void testGrowing() {
    }
}
