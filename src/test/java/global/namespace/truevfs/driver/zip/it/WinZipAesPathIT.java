/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.it;

import global.namespace.truevfs.access.TConfig;
import global.namespace.truevfs.access.TPath;
import global.namespace.truevfs.access.it.TPathITSuite;
import global.namespace.truevfs.comp.key.spec.prompting.TestView;
import global.namespace.truevfs.comp.zipdriver.TestWinZipAesDriver;
import org.junit.Test;

import java.io.IOException;

import static global.namespace.truevfs.comp.key.spec.prompting.TestView.Action.CANCEL;
import static global.namespace.truevfs.comp.key.spec.prompting.TestView.Action.ENTER;
import static global.namespace.truevfs.kernel.api.FsAccessOption.ENCRYPT;
import static java.nio.file.Files.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 */
public final class WinZipAesPathIT extends TPathITSuite<TestWinZipAesDriver> {

    @Override
    protected String getExtensionList() { return "zip"; }

    @Override
    protected TestWinZipAesDriver newArchiveDriver() {
        return new TestWinZipAesDriver();
    }

    @Override
    public void setUp() throws IOException {
        super.setUp();
        TConfig.current().setAccessPreference(ENCRYPT, true);
    }

    private void setAction(TestView.Action action) {
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
