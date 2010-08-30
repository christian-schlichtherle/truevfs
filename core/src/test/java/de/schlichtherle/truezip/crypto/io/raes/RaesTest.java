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

package de.schlichtherle.truezip.crypto.io.raes;

import de.schlichtherle.truezip.crypto.generators.DigestRandom;
import de.schlichtherle.truezip.io.rof.ReadOnlyFileTestCase;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.crypto.digests.SHA256Digest;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class RaesTest extends ReadOnlyFileTestCase {

    private static final Logger logger = Logger.getLogger(
            RaesTest.class.getName());

    private static final String PASSWD = "secret";

    private static final Random rnd = new DigestRandom(new SHA256Digest());

    private static final int[] keyStrengths = {
        Type0RaesParameters.KEY_STRENGTH_128,
        Type0RaesParameters.KEY_STRENGTH_192,
        Type0RaesParameters.KEY_STRENGTH_256
    };

    private static RaesParameters newRaesParameters() {
        return new Type0RaesParameters() {
            boolean secondTry;

            public char[] getOpenPasswd() {
                if (secondTry) {
                    logger.finer("First returned password was wrong, providing the right one now!");
                    return PASSWD.toCharArray();
                } else {
                    secondTry = true;
                    return rnd.nextBoolean()
                            ? PASSWD.toCharArray()
                            : "wrong".toCharArray();
                }
            }

            public void invalidOpenPasswd() {
                logger.finer("Password wrong!");
            }

            public char[] getCreatePasswd() {
                return PASSWD.toCharArray();
            }

            public int getKeyStrength() {
                return keyStrengths[rnd.nextInt(keyStrengths.length)];
            }

            public void setKeyStrength(int keyStrength) {
                logger.log(Level.FINER, "Key strength: {0}", keyStrength);
            }
        };
    }

    private File cipherFile;

    public RaesTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp()
    throws IOException {
        super.setUp();

        cipherFile = File.createTempFile("tmp", null);
        try {
            final RaesOutputStream out = RaesOutputStream.getInstance(
                    new FileOutputStream(cipherFile),
                    newRaesParameters());
            try {
                // Use Las Vegas algorithm to encrypt the data.
                int n;
                for (int off = 0; off < data.length; off += n) {
                    if (rnd.nextBoolean()) {
                        // Write a byte.
                        n = 1;
                        out.write(data[off]);
                    } else {
                        // Write a buffer (maybe zero length).
                        n = rnd.nextInt(data.length / 100);
                        n = Math.min(n, data.length - off);
                        out.write(data, off, n);
                    }
                    if (rnd.nextBoolean())
                        out.flush(); // maybe flush
                }
            } finally {
                out.close();
            }
            logger.log(Level.FINE,
                    "Encrypted {0} bytes of random data using AES-{1}/CTR/Hmac-SHA-256/PBKDFv2.",
                    new Object[]{ data.length, out.getKeySizeBits() });
            // Open cipherFile for random access decryption.
            trof = RaesReadOnlyFile.getInstance(cipherFile, newRaesParameters());
        } catch (IOException ex) {
            if (!cipherFile.delete())
                cipherFile.deleteOnExit();
            throw ex;
        }
    }

    @Override
    protected void tearDown()
    throws IOException {
        try {
            super.tearDown();
        } finally {
            try {
                if (!cipherFile.delete() && cipherFile.exists()) {
                    cipherFile.deleteOnExit();
                    throw new IOException(cipherFile + ": could not delete");
                }
            } finally {
                cipherFile = null;
            }
        }
    }
}
