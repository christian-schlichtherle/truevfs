/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.crypto;

import de.truezip.driver.zip.crypto.CipherOutputStream;
import de.truezip.key.param.KeyStrength;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.OutputStream;
import javax.annotation.concurrent.NotThreadSafe;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.Mac;

/**
 * An {@link OutputStream} to produce a file with data ecnrypted according
 * to the Random Access Encryption Specification (RAES).
 *
 * @see    RaesReadOnlyChannel
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class RaesOutputStream extends CipherOutputStream {

    @CreatesObligation
    RaesOutputStream(BufferedBlockCipher cipher) {
        super(null, cipher);
    }

    /**
     * Returns the key strength which is actually used to encrypt the data of
     * the RAES file.
     * 
     * @return The key strength which is actually used to encrypt the data of
     *         the RAES file.
     */
    public abstract KeyStrength getKeyStrength();

    /**
     * Update the given KLAC with the given file {@code length} in
     * little endian order and finish it, writing the result to {@code buf}.
     * The KLAC must already have been initialized and updated with the
     * password bytes as retrieved according to PKCS #12.
     * The result is stored in {@code buf}, which must match the given
     * KLAC's output size.
     */
    static void klac(final Mac klac, long size, final byte[] buf) {
        for (int i = 0; i < 8; i++) {
            klac.update((byte) size);
            size >>= 8;
        }
        final int bufLength = klac.doFinal(buf, 0);
        assert bufLength == buf.length;
    }
}
