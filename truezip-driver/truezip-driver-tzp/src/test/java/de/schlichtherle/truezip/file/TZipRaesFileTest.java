/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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

import de.schlichtherle.truezip.key.PromptingKeyManager;
import java.util.ServiceConfigurationError;
import de.schlichtherle.truezip.key.PromptingKeyProvider;
import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.key.KeyManagerService;
import java.io.File;
import de.schlichtherle.truezip.fs.archive.zip.raes.SafeZipRaesDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.archive.zip.raes.ZipRaesDriver.KeyProviderSyncStrategy;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyPromptingCancelledException;
import de.schlichtherle.truezip.key.UnknownKeyException;
import java.io.IOException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class TZipRaesFileTest extends TFileTestSuite {

    private static boolean cancelling;

    public TZipRaesFileTest() {
        super(  FsScheme.create("tzp"),
                new SafeZipRaesDriver(POOL_SERVICE, new CustomKeyManagerService()) {
            @Override
            public KeyProviderSyncStrategy getKeyProviderSyncStrategy() {
                return KeyProviderSyncStrategy.RESET_UNCONDITIONALLY;
            }
        });
    }

    @Override
    public void setUp() throws IOException {
        cancelling = false;
        super.setUp();
    }

    @Test
    public void testCancelling() throws IOException {
        cancelling = true;

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
        cancelling = true;
        assertTrue(archive.exists());
        assertFalse(archive.isDirectory());
        assertFalse(archive.isFile());

        TFile.umount();
        cancelling = false;
        assertTrue(archive.exists());
        assertTrue(archive.isDirectory());
        assertFalse(archive.isFile());

        cancelling = true;
        assertTrue(inner.exists());
        assertFalse(inner.isDirectory());
        assertFalse(inner.isFile());

        TFile.umount();
        assertFalse(archive.deleteAll());

        TFile.umount();
        cancelling = false;
        assertTrue(archive.deleteAll());
    }

    private static class CustomKeyManagerService implements KeyManagerService {

        private static final PromptingKeyManager<AesCipherParameters>
                manager = new PromptingKeyManager<AesCipherParameters>(
                    new CustomUI());

        @Override
        @SuppressWarnings("unchecked")
        public <K> KeyManager<? extends K, ?> getKeyManager(Class<K> type) {
            if (type.isAssignableFrom(AesCipherParameters.class))
                return (KeyManager<? extends K, ?>) manager;
            throw new ServiceConfigurationError("No service available for " + type);
        }
    } // CustomKeyManagerService

    private static class CustomUI implements PromptingKeyProvider.UI<AesCipherParameters> {

        @Override
        public void promptCreateKey(
                PromptingKeyProvider<? super AesCipherParameters> provider)
        throws UnknownKeyException {
            if (cancelling)
                throw new KeyPromptingCancelledException();
            AesCipherParameters param = new AesCipherParameters();
            param.setPassword("secret".toCharArray());
            provider.setKey(param);
        }

        @Override
        public void promptOpenKey(
                PromptingKeyProvider<? super AesCipherParameters> provider,
                boolean invalid)
        throws UnknownKeyException {
            promptCreateKey(provider);
        }
    } // class UI
}
