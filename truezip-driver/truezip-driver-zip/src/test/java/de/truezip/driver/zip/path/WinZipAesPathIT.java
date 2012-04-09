/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.path;

import de.truezip.driver.zip.TestWinZipAesDriver;
import de.truezip.file.TConfig;
import static de.truezip.kernel.option.AccessOption.ENCRYPT;
import de.truezip.key.MockView;
import static de.truezip.key.MockView.Action.CANCEL;
import static de.truezip.key.MockView.Action.ENTER;
import de.truezip.path.TPath;
import de.truezip.path.TPathITSuite;
import java.io.IOException;
import static java.nio.file.Files.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public final class WinZipAesPathIT extends TPathITSuite<TestWinZipAesDriver> {

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

    private void setAction(MockView.Action action) {
        getArchiveDriver().getView().setAction(action);
    }

    @Test
    public void testCancelling() throws IOException {
        setAction(CANCEL);

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
        final TPath inner = archive.resolve("inner" + getExtension());

        createDirectory(archive);
        createDirectory(inner);

        umount();
        setAction(CANCEL);
        assertTrue(exists(archive));
        assertTrue(isDirectory(archive));
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
}