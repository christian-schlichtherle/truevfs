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
package de.schlichtherle.truezip.crypto.raes;

import de.schlichtherle.truezip.crypto.raes.RaesOutputStream;
import de.schlichtherle.truezip.crypto.raes.RaesReadOnlyFile;
import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters;
import de.schlichtherle.truezip.crypto.raes.RaesParameters;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFileTestCase;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;
import org.bouncycastle.crypto.prng.RandomGenerator;
import org.junit.After;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class RaesTest extends ReadOnlyFileTestCase {

    private static final Logger logger = Logger.getLogger(
            RaesTest.class.getName());

    private static final String PASSWD = "secret";

    private static final RandomGenerator rng
            = new DigestRandomGenerator(new SHA256Digest());

    static {
        rng.addSeedMaterial(System.currentTimeMillis());
    }

    private static final int[] keyStrengths = {
        Type0RaesParameters.KEY_STRENGTH_128,
        Type0RaesParameters.KEY_STRENGTH_192,
        Type0RaesParameters.KEY_STRENGTH_256
    };

    private static RaesParameters newRaesParameters() {
        return new Type0RaesParameters() {
            boolean secondTry;

            @Override
            public char[] getOpenPasswd() {
                if (secondTry) {
                    logger.finer("First returned password was wrong, providing the right one now!");
                    return PASSWD.toCharArray();
                } else {
                    secondTry = true;
                    byte[] buf = new byte[1];
                    rng.nextBytes(buf);
                    return buf[0] >= 0
                            ? PASSWD.toCharArray()
                            : "wrong".toCharArray();
                }
            }

            @Override
            public void invalidOpenPasswd() {
                logger.finer("Password wrong!");
            }

            @Override
            public char[] getCreatePasswd() {
                return PASSWD.toCharArray();
            }

            @Override
            public int getKeyStrength() {
                byte[] buf = new byte[1];
                rng.nextBytes(buf);
                return keyStrengths[(buf[0] & 0xFF) % keyStrengths.length];
            }

            @Override
            public void setKeyStrength(int keyStrength) {
                logger.log(Level.FINER, "Key strength: {0}", keyStrength);
            }
        };
    }

    private File cipherFile;

    @Override
    protected ReadOnlyFile newReadOnlyFile(final File plainFile)
    throws IOException {
        final InputStream in = new FileInputStream(plainFile);
        try {
            cipherFile = File.createTempFile(TEMP_FILE_PREFIX, null);
            try {
                final RaesOutputStream out = RaesOutputStream.getInstance(
                        new FileOutputStream(cipherFile),
                        newRaesParameters());
                Streams.copy(in, out);
                logger.log(Level.FINE,
                        "Encrypted {0} bytes of random data using AES-{1}/CTR/Hmac-SHA-256/PBKDFv2.",
                        new Object[]{ plainFile.length(), out.getKeySizeBits() });
                // Open cipherFile for random access decryption.
            } catch (IOException ex) {
                final File cipherFile = this.cipherFile;
                this.cipherFile = null;
                if (!cipherFile.delete())
                    throw new IOException(cipherFile + " (could not delete)", ex);
                throw ex;
            }
            return RaesReadOnlyFile.getInstance(cipherFile, newRaesParameters());
        } finally {
            in.close();
        }
    }

    @Override
    public void tearDown() throws IOException {
        try {
            super.tearDown();
        } finally {
            final File cipherFile = this.cipherFile;
            this.cipherFile = null;
            if (null != cipherFile && cipherFile.exists() && !cipherFile.delete())
                logger.log(Level.WARNING, "{0} (File.delete() failed)", cipherFile);
        }
    }
}
