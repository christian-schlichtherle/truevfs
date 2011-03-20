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
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.key.MockView;
import de.schlichtherle.truezip.key.PromptingKeyProvider.View;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import java.util.ServiceConfigurationError;
import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import java.io.File;
import de.schlichtherle.truezip.fs.archive.zip.raes.SafeZipRaesDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.archive.zip.raes.ZipRaesDriver.KeyProviderSyncStrategy;
import de.schlichtherle.truezip.key.KeyManager;
import java.io.IOException;
import org.junit.Test;

import static de.schlichtherle.truezip.key.MockView.Action.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class TZipRaesFileTest extends TFileTestSuite {

    private static final MockView<AesCipherParameters>
            view = new MockView<AesCipherParameters>();

    public TZipRaesFileTest() {
        super(  FsScheme.create("tzp"),
                new SafeZipRaesDriver(POOL_PROVIDER, new MockKeyManagerProvider(view)) {
            @Override
            public KeyProviderSyncStrategy getKeyProviderSyncStrategy() {
                return KeyProviderSyncStrategy.RESET_UNCONDITIONALLY;
            }
        });
    }

    @Override
    public void setUp() throws IOException {
        final AesCipherParameters key = new AesCipherParameters();
        key.setPassword("secret".toCharArray());
        view.setKey(key);
        view.setAction(ENTER);
        super.setUp();
    }

    @Test
    public void testCancelling() throws IOException {
        view.setAction(CANCEL);

        final TFile archive = new TFile(getArchive().getPath()); // recreate with passwdCancellingDetector
        final TFile entry1 = new TFile(archive, "entry1");

        assertFalse(new File(archive.getPath()).exists());

        assertFalse(entry1.mkdirs());
        assertFalse(new File(entry1.getPath()).exists());
        assertFalse(new File(archive.getPath()).exists());

        try {
            assertFalse(entry1.createNewFile());
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        assertFalse(new File(entry1.getPath()).exists());
        assertFalse(new File(archive.getPath()).exists());

        final TFile entry2 = new TFile(entry1, "entry2");
        assertFalse(entry2.mkdirs());
        assertFalse(new File(entry2.getPath()).exists());
        assertFalse(new File(entry1.getPath()).exists());
        assertFalse(new File(archive.getPath()).exists());

        try {
            assertFalse(entry2.createNewFile());
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        assertFalse(new File(entry2.getPath()).exists());
        assertFalse(new File(entry1.getPath()).exists());
        assertFalse(new File(archive.getPath()).exists());
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
        assertFalse(archive.deleteAll());

        TFile.umount();
        view.setAction(ENTER);
        assertTrue(archive.deleteAll());
    }

    private static class MockKeyManagerProvider implements KeyManagerProvider {

        private final PromptingKeyManager<AesCipherParameters> manager;

        public MockKeyManagerProvider(View<AesCipherParameters> view) {
            this.manager = new PromptingKeyManager<AesCipherParameters>(view);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K> KeyManager<? extends K, ?> get(Class<K> type) {
            if (type.isAssignableFrom(AesCipherParameters.class))
                return (KeyManager<? extends K, ?>) manager;
            throw new ServiceConfigurationError("No key manager available for " + type);
        }
    } // CustomKeyManagerService
}
