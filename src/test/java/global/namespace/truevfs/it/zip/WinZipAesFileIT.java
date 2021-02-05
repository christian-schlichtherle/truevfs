/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.zip;

import global.namespace.truevfs.access.TConfig;
import global.namespace.truevfs.access.TFile;
import global.namespace.truevfs.commons.key.api.prompting.TestView.Action;
import global.namespace.truevfs.commons.zipdriver.TestWinZipAesDriver;
import global.namespace.truevfs.it.base.TFileITSuite;
import org.junit.Test;

import java.io.IOException;

import static global.namespace.truevfs.commons.key.api.prompting.TestView.Action.CANCEL;
import static global.namespace.truevfs.commons.key.api.prompting.TestView.Action.ENTER;
import static global.namespace.truevfs.kernel.api.FsAccessOption.ENCRYPT;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 */
public final class WinZipAesFileIT extends TFileITSuite<TestWinZipAesDriver> {

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
