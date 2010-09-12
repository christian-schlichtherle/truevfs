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

import de.schlichtherle.truezip.key.AesKeyProvider;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyPromptingCancelledException;
import de.schlichtherle.truezip.key.UnknownKeyException;
import java.io.IOException;

/**
 * Tests the TrueZIP API in the package {@code de.schlichtherle.truezip.io} with the
 * ZIP.RAES (TZP) driver.
 * This test uses a custom key manager in order to automatically provide a
 * constant password without prompting the user.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ZipRaesFileTest extends FileTestCase {

    private static boolean cancelling;

    private KeyManager oldKeyManager;

    /** Creates a new instance of {@code ZipRaesFileTest}. */
    public ZipRaesFileTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        suffix = ".tzp";
        File.setDefaultArchiveDetector(new DefaultArchiveDetector("tzp"));
        oldKeyManager = KeyManager.getInstance();
        KeyManager.setInstance(new CustomKeyManager());
        cancelling = false;
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        KeyManager.setInstance(oldKeyManager);
    }

    public void testCancelling()
    throws IOException {
        cancelling = true;

        archive = new File(archive.getPath()); // recreate with passwdCancellingDetector
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

    public void testFileStatus()
    throws IOException {
        final File inner = new File(archive, "inner" + suffix);

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
    }

    public static class CustomKeyManager extends KeyManager {
        public CustomKeyManager() {
            mapKeyProviderType(AesKeyProvider.class, SimpleAesKeyProvider.class);
        }
    }

    public static class SimpleAesKeyProvider implements AesKeyProvider<char[]> {
        public char[] getCreateKey() throws UnknownKeyException {
            if (cancelling)
                throw new KeyPromptingCancelledException();
            return "secret".toCharArray(); // return clone!
        }

        public char[] getOpenKey() throws UnknownKeyException {
            if (cancelling)
                throw new KeyPromptingCancelledException();
            return "secret".toCharArray(); // return clone!
        }

        public void invalidOpenKey() {
            throw new AssertionError(
                    "Illegal call: Key is constant or password prompting has been cancelled!");
        }

        public int getKeyStrength() {
            return KEY_STRENGTH_256;
        }

        public void setKeyStrength(int keyStrength) {
        }
    }
}
