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
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.crypto.CipherOutputStream;
import de.schlichtherle.truezip.crypto.param.KeyStrength;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.NotThreadSafe;
import org.bouncycastle.crypto.BufferedBlockCipher;

/**
 * An {@link OutputStream} for producing a ZIP file with data ecnrypted
 * according to different specifications.
 *
 * @see     ZipCryptoInputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
abstract class ZipCryptoOutputStream
extends CipherOutputStream {

    /**
     * Returns a new {@code ZipCryptoOutputStream}.
     *
     * @param  out The underlying output stream to use for the encrypted data.
     * @param  param The {@link ZipCryptoParameters} used to determine and
     *         configure the type of the encrypted ZIP file.
     *         If the run time class of this parameter matches multiple
     *         parameter interfaces, it is at the discretion of this
     *         implementation which one is picked and hence which type of
     *         encrypted ZIP file is created.
     *         If you need more control over this, pass in an instance which's
     *         run time class just implements the
     *         {@link ZipCryptoParametersProvider} interface.
     *         Instances of this interface are queried to find crypto
     *         parameters which match a known encrypted ZIP file type.
     *         This algorithm is recursively applied.
     * @return A new {@code ZipCryptoOutputStream}.
     * @throws RaesParametersException If {@code param} is {@code null} or
     *         no suitable crypto parameters can be found.
     * @throws IOException On any I/O error.
     */
    public static ZipCryptoOutputStream getInstance(
            final OutputStream out,
            final @CheckForNull ZipCryptoParameters param)
    throws IOException {
        if (null == out)
            throw new NullPointerException();
        // Order is important here to support multiple interface implementations!
        if (param == null) {
            throw new ZipCryptoParametersException("No crypto parameters available!");
        } else if (param instanceof WinZipAesParameters) {
            /*return new Type0RaesOutputStream(out,
                    (Type0RaesParameters) param);*/
            throw new UnsupportedOperationException();
        } else if (param instanceof ZipCryptoParametersProvider) {
            return getInstance(out,
                    ((ZipCryptoParametersProvider) param).get(ZipCryptoParameters.class));
        } else {
            throw new ZipCryptoParametersException();
        }
    }

    ZipCryptoOutputStream(
            OutputStream out,
            @CheckForNull BufferedBlockCipher cipher) {
        super(out, cipher);
    }

    /**
     * Returns the key strength which is actually used to encrypt the data of
     * the ZIP file.
     */
    public abstract KeyStrength getKeyStrength();
}
