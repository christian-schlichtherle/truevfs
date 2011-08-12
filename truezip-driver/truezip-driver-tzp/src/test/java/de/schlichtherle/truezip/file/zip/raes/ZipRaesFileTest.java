/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.file.zip.raes;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileTestSuite;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.archive.zip.raes.PromptingKeyManagerService;
import de.schlichtherle.truezip.fs.archive.zip.raes.SafeZipRaesDriver;
import de.schlichtherle.truezip.key.MockView;
import static de.schlichtherle.truezip.key.MockView.Action.*;
import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class ZipRaesFileTest extends TFileTestSuite {

    private static final MockView<AesCipherParameters>
            view = new MockView<AesCipherParameters>();

    public ZipRaesFileTest() {
        super(  FsScheme.create("tzp"),
                new SafeZipRaesDriver(  IO_POOL_PROVIDER,
                                        new PromptingKeyManagerService(view)) {
            @Override
            public KeyProviderSyncStrategy getKeyProviderSyncStrategy() {
                return KeyProviderSyncStrategy.RESET_UNCONDITIONALLY;
            }
        });
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final AesCipherParameters key = new AesCipherParameters();
        key.setPassword("secret".toCharArray());
        view.setKey(key);
        view.setAction(ENTER);
    }

    @Test
    public void testCancelling() throws IOException {
        view.setAction(CANCEL);

        final TFile archive = getArchive();
        final TFile entry1 = new TFile(archive, "entry1");

        assertFalse(newNonArchiveFile(archive).exists());

        assertFalse(entry1.mkdirs());
        assertFalse(newNonArchiveFile(entry1).exists());
        assertFalse(newNonArchiveFile(archive).exists());

        try {
            assertFalse(entry1.createNewFile());
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        assertFalse(newNonArchiveFile(entry1).exists());
        assertFalse(newNonArchiveFile(archive).exists());

        final TFile entry2 = new TFile(entry1, "entry2");
        assertFalse(entry2.mkdirs());
        assertFalse(newNonArchiveFile(entry2).exists());
        assertFalse(newNonArchiveFile(entry1).exists());
        assertFalse(newNonArchiveFile(archive).exists());

        try {
            assertFalse(entry2.createNewFile());
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        assertFalse(newNonArchiveFile(entry2).exists());
        assertFalse(newNonArchiveFile(entry1).exists());
        assertFalse(newNonArchiveFile(archive).exists());
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

    @Override
    public void testGrow() {
        // GROWing is not supported with ZIP.RAES.
    }
}
