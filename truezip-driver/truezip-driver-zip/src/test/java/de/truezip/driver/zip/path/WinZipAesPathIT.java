/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.path;

import de.truezip.driver.zip.TestWinZipAesDriver;
import static de.truezip.key.MockView.Action.CANCEL;
import static de.truezip.key.MockView.Action.ENTER;
import de.truezip.path.TPath;
import de.truezip.path.TPathTestSuite;
import java.io.IOException;
import static java.nio.file.Files.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public final class WinZipAesPathIT extends TPathTestSuite<TestWinZipAesDriver> {
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

        final TPath archive = getArchive();
        assertFalse(exists(archive.toNonArchivePath()));

        final TPath entry1 = archive.resolve("entry1");
        createDirectories(entry1);
        delete(entry1);
        createFile(entry1);
        delete(entry1);

        final TPath entry2 = archive.resolve("entry2");
        createDirectories(entry2);
        delete(entry2);
        createFile(entry2);
        delete(entry2);
    }

    @Test
    public void testFileStatus() throws IOException {
        final TPath archive = getArchive();
        final TPath inner = archive.resolve("inner" + getSuffix());

        createDirectory(archive);
        createDirectory(inner);

        umount();
        getArchiveDriver().getView().setAction(CANCEL);
        assertTrue(exists(archive));
        assertTrue(isDirectory(archive));
        assertFalse(isRegularFile(archive));

        umount();
        getArchiveDriver().getView().setAction(ENTER);
        assertTrue(exists(archive));
        assertTrue(isDirectory(archive));
        assertFalse(isRegularFile(archive));

        getArchiveDriver().getView().setAction(CANCEL);
        assertTrue(exists(inner));
        assertTrue(isDirectory(inner));
        assertFalse(isRegularFile(inner));

        umount();
        archive.toFile().rm_r();
    }
}