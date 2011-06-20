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
package de.schlichtherle.truezip.nio.file;

import de.schlichtherle.truezip.key.MockView;
import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import static java.nio.file.Files.*;
import de.schlichtherle.truezip.fs.archive.zip.raes.SafeZipRaesDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.archive.zip.raes.PromptingKeyManagerService;
import java.io.IOException;
import org.junit.Test;

import static de.schlichtherle.truezip.key.MockView.Action.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class TZipRaesPathTest extends TPathTestSuite {

    private static final MockView<AesCipherParameters>
            view = new MockView<AesCipherParameters>();

    public TZipRaesPathTest() {
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

        final TPath archive = getArchive();
        final TPath entry1 = archive.resolve("entry1");

        assertFalse(exists(newNonArchiveFile(archive)));

        try {
            createDirectory(entry1);
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        assertFalse(exists(newNonArchiveFile(entry1)));
        assertFalse(exists(newNonArchiveFile(archive)));

        try {
            createFile(entry1);
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        assertFalse(exists(newNonArchiveFile(entry1)));
        assertFalse(exists(newNonArchiveFile(archive)));

        final TPath entry2 = entry1.resolve("entry2");
        try {
            createDirectory(entry2);
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        assertFalse(exists(newNonArchiveFile(entry2)));
        assertFalse(exists(newNonArchiveFile(entry1)));
        assertFalse(exists(newNonArchiveFile(archive)));

        try {
            createFile(entry2);
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        assertFalse(exists(newNonArchiveFile(entry2)));
        assertFalse(exists(newNonArchiveFile(entry1)));
        assertFalse(exists(newNonArchiveFile(archive)));
    }

    @Test
    public void testFileStatus() throws IOException {
        final TPath archive = getArchive();
        final TPath inner = archive.resolve("inner" + getSuffix());

        createDirectory(archive);
        createDirectory(inner);

        TFileSystem.umount();
        view.setAction(CANCEL);
        assertTrue(exists(archive));
        assertFalse(isDirectory(archive));
        assertFalse(isRegularFile(archive));

        TFileSystem.umount();
        view.setAction(ENTER);
        assertTrue(exists(archive));
        assertTrue(isDirectory(archive));
        assertFalse(isRegularFile(archive));

        view.setAction(CANCEL);
        assertTrue(exists(inner));
        assertFalse(isDirectory(inner));
        assertFalse(isRegularFile(inner));

        TFileSystem.umount();
        try {
            archive.toFile().rm_r();
            fail();
        } catch (IOException expected) {
        }
            
        TFileSystem.umount();
        view.setAction(ENTER);
        archive.toFile().rm_r();
    }
}
