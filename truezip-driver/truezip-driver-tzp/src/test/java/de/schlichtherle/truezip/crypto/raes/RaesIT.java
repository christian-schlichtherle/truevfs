/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.crypto.raes;

import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFileTestBase;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class RaesIT extends ReadOnlyFileTestBase {

    private static final Logger logger = Logger.getLogger(
            RaesIT.class.getName());

    private static RaesParameters newRaesParameters() {
        return new MockType0RaesParameters();
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
                        "Encrypted {0} bytes of random data using AES-{1}/CTR/Hmac-SHA-256/PKCS#12v1",
                        new Object[]{ plainFile.length(), out.getKeyStrength().getBits() });
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
    public void tearDown() {
        try {
            try {
                super.tearDown();
            } finally {
                final File cipherFile = this.cipherFile;
                this.cipherFile = null;
                if (null != cipherFile && cipherFile.exists() && !cipherFile.delete())
                    throw new IOException(cipherFile + " (could not delete)");
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, ex.toString(), ex);
        }
    }
}
