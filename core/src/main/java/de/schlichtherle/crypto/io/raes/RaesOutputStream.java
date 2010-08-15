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

package de.schlichtherle.crypto.io.raes;

import de.schlichtherle.crypto.io.*;

import java.io.*;

import org.bouncycastle.crypto.*;

/**
 * An {@link OutputStream} to produce a file with data ecnrypted according
 * to the Random Access Encryption Specification (RAES).
 * 
 * @see RaesReadOnlyFile
 *
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 6.0
 */
public abstract class RaesOutputStream extends CipherOutputStream {

    /**
     * Update the given KLAC with the given file <tt>length</tt> in
     * little endian order and finalize it, writing the result to <tt>buf</tt>.
     * The KLAC must already have been initialized and updated with the
     * password bytes as retrieved according to PKCS #12.
     * The result is stored in <tt>buf</tt>, which must match the given
     * KLAC's output size.
     */
    static void klac(final Mac klac, long length, final byte[] buf) {
        for (int i = 0; i < 8; i++) {
            klac.update((byte) length);
            length >>= 8;
        }
        final int bufLen = klac.doFinal(buf, 0);
        assert bufLen == buf.length;
    }

    /**
     * Creates a new instance of <code>RaesOutputStream</code>.
     * 
     * @param out The underlying output stream to use for the encrypted data.
     * @param parameters The {@link RaesParameters} used to determine and
     *        configure the type of RAES file created.
     *        If the run time class of this parameter matches multiple
     *        parameter interfaces, it is at the discretion of this
     *        implementation which one is picked and hence which type of
     *        RAES file is created.
     *        If you need more control over this, pass in an instance which's
     *        run time class just implements the
     *        {@link RaesParametersAgent} interface.
     *        Instances of this interface are recursively used to find RAES
     *        parameters which match a known RAES type.
     * @throws NullPointerException If {@link #out} is <tt>null</tt>
     *         or <tt>parameters</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException If an illegal keyStrength is provided
     *         in the parameters.
     * @throws RaesParametersException If no suitable RAES parameters have been
     *         provided or something is wrong with the parameters.
     * @throws IOException On any other I/O related issue.
     */
    public static RaesOutputStream getInstance(
            final OutputStream out,
            RaesParameters parameters)
    throws  NullPointerException,
            IllegalArgumentException,
            RaesParametersException,
            IOException {
        if (out == null)
            throw new NullPointerException("out");

        // Order is important here to support multiple interface implementations!
        if (parameters == null) {
            throw new RaesParametersException();
        } else if (parameters instanceof Type0RaesParameters) {
            return new Type0RaesOutputStream(out,
                    (Type0RaesParameters) parameters);
        } else if (parameters instanceof RaesParametersAgent) {
            parameters = ((RaesParametersAgent) parameters).getParameters(
                    RaesParameters.class);
            return getInstance(out, parameters);
        } else {
            throw new RaesParametersException();
        }
    }

    RaesOutputStream(OutputStream out, BufferedBlockCipher cipher) {
        super(out, cipher);
    }

    /**
     * Returns the key size in bits which is actually used to encrypt or
     * decrypt the data for this output stream.
     */
    public abstract int getKeySizeBits();
}
