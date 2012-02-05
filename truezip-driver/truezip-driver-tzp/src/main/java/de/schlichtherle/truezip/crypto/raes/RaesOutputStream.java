/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.crypto.raes;

import de.schlichtherle.truezip.crypto.CipherOutputStream;
import de.schlichtherle.truezip.crypto.param.KeyStrength;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.Mac;

/**
 * An {@link OutputStream} to produce a file with data ecnrypted according
 * to the Random Access Encryption Specification (RAES).
 *
 * @see     RaesReadOnlyFile
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public abstract class RaesOutputStream extends CipherOutputStream {

    /**
     * Update the given KLAC with the given file {@code length} in
     * little endian order and finalize it, writing the result to {@code buf}.
     * The KLAC must already have been initialized and updated with the
     * password bytes as retrieved according to PKCS #12.
     * The result is stored in {@code buf}, which must match the given
     * KLAC's output size.
     */
    static void klac(final Mac klac, long length, final byte[] buf) {
        for (int i = 0; i < 8; i++) {
            klac.update((byte) length);
            length >>= 8;
        }
        final int bufLength = klac.doFinal(buf, 0);
        assert bufLength == buf.length;
    }

    /**
     * Returns a new {@code RaesOutputStream}.
     *
     * @param  out the output stream to decorate for writing the ciphered data.
     * @param  param The {@link RaesParameters} used to determine and
     *         configure the type of RAES file created.
     *         If the run time class of this parameter matches multiple
     *         parameter interfaces, it is at the discretion of this
     *         implementation which one is picked and hence which type of
     *         RAES file is created.
     *         If you need more control over this, pass in an instance which's
     *         run time class just implements the
     *         {@link RaesParametersProvider} interface.
     *         Instances of this interface are queried to find RAES parameters
     *         which match a known RAES type.
     *         This algorithm is recursively applied.
     * @return A new {@code RaesOutputStream}.
     * @throws RaesParametersException if {@code param} is {@code null} or
     *         no suitable RAES parameters can be found.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public static RaesOutputStream getInstance(
            final @WillCloseWhenClosed OutputStream out,
            @CheckForNull RaesParameters param)
    throws IOException {
        if (null == out)
            throw new NullPointerException();
        while (null != param) {
            // Order is important here to support multiple interface implementations!
            if (param instanceof Type0RaesParameters) {
                return new Type0RaesOutputStream(out,
                        (Type0RaesParameters) param);
            } else if (param instanceof RaesParametersProvider) {
                param = ((RaesParametersProvider) param)
                        .get(RaesParameters.class);
            } else {
                break;
            }
        }
        throw new RaesParametersException("No suitable RAES parameters available!");
    }

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    RaesOutputStream(   @CheckForNull @WillCloseWhenClosed OutputStream out,
                        @CheckForNull BufferedBlockCipher cipher) {
        super(out, cipher);
    }

    /**
     * Returns the key strength which is actually used to encrypt the data of
     * the RAES file.
     */
    public abstract KeyStrength getKeyStrength();
}
