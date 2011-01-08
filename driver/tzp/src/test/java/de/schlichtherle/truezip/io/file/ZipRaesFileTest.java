/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.file;

import de.schlichtherle.truezip.io.fs.archive.driver.zip.raes.SafeZipRaesDriver;
import de.schlichtherle.truezip.io.fs.FSScheme1;
import de.schlichtherle.truezip.key.AesKeyProvider;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyPromptingCancelledException;
import de.schlichtherle.truezip.key.UnknownKeyException;
import java.io.IOException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the TrueZIP API in the package {@code de.schlichtherle.truezip.io} with the
 * ZIP.RAES (TZP) driver.
 * This test uses a custom key manager in order to automatically provide a
 * constant password without prompting the user.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class ZipRaesFileTest extends FileTestCase {

    private static boolean cancelling;

    public ZipRaesFileTest() {
        super(FSScheme1.create("tzp"), new SafeZipRaesDriver());
    }

    @Override
    public void setUp() throws IOException {
        KeyManager.setInstance(new CustomKeyManager());
        cancelling = false;
        super.setUp();
    }

    @Override
    public void tearDown() throws IOException {
        //cancelling = false;
        super.tearDown();
        KeyManager.resetAndRemoveKeyProviders();
        KeyManager.setInstance(null);
    }

    @Test
    public void testCancelling() throws IOException {
        cancelling = true;

        final File archive = new File(getArchive().getPath()); // recreate with passwdCancellingDetector
        final File entry1 = new File(archive, "entry1");

        assertFalse(new java.io.File(archive.getPath()).exists());

        assertFalse(entry1.mkdirs());
        assertFalse(new java.io.File(entry1.getPath()).exists());
        assertFalse(new java.io.File(archive.getPath()).exists());

        try {
            assertFalse(entry1.createNewFile());
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        assertFalse(new java.io.File(entry1.getPath()).exists());
        assertFalse(new java.io.File(archive.getPath()).exists());

        final File entry2 = new File(entry1, "entry2");
        assertFalse(entry2.mkdirs());
        assertFalse(new java.io.File(entry2.getPath()).exists());
        assertFalse(new java.io.File(entry1.getPath()).exists());
        assertFalse(new java.io.File(archive.getPath()).exists());

        try {
            assertFalse(entry2.createNewFile());
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        assertFalse(new java.io.File(entry2.getPath()).exists());
        assertFalse(new java.io.File(entry1.getPath()).exists());
        assertFalse(new java.io.File(archive.getPath()).exists());
    }

    @Test
    public void testFileStatus() throws IOException {
        final File archive = getArchive();
        final File inner = new File(archive, "inner" + getSuffix());

        assertTrue(archive.mkdir());
        assertTrue(inner.mkdir());

        File.umount();

        cancelling = true;
        assertTrue(archive.exists());
        assertFalse(archive.isDirectory());
        assertFalse(archive.isFile());

        cancelling = false;
        assertTrue(archive.exists());
        assertTrue(archive.isDirectory());
        assertFalse(archive.isFile());

        cancelling = true;
        assertTrue(inner.exists());
        assertFalse(inner.isDirectory());
        assertFalse(inner.isFile());

        assertFalse(archive.deleteAll());

        cancelling = false;
        assertTrue(archive.deleteAll());
    }

    public static class CustomKeyManager extends KeyManager {
        //@SuppressWarnings("unchecked")
        public CustomKeyManager() {
            mapKeyProviderType(AesKeyProvider.class, SimpleAesKeyProvider.class);
        }
    }

    public static class SimpleAesKeyProvider implements AesKeyProvider<char[]> {
        @Override
		public char[] getCreateKey() throws UnknownKeyException {
            if (cancelling)
                throw new KeyPromptingCancelledException();
            return "secret".toCharArray(); // return clone!
        }

        @Override
		public char[] getOpenKey() throws UnknownKeyException {
            if (cancelling)
                throw new KeyPromptingCancelledException();
            return "secret".toCharArray(); // return clone!
        }

        @Override
		public void invalidOpenKey() {
            throw new AssertionError(
                    "Illegal call: Key is constant or password prompting has been cancelled!");
        }

        @Override
		public int getKeyStrength() {
            return KEY_STRENGTH_256;
        }

        @Override
		public void setKeyStrength(int keyStrength) {
        }
    }
}
