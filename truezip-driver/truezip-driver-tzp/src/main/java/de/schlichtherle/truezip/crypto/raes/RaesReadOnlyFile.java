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

import de.schlichtherle.truezip.crypto.CipherReadOnlyFile;
import static de.schlichtherle.truezip.crypto.raes.RaesConstants.*;
import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.DefaultReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;

/**
 * This class implements a {@link de.schlichtherle.truezip.rof.ReadOnlyFile}
 * in order to provide transparent random read only access to the plain text
 * data which has been encrypted and stored in a file according to the
 * Random Access Encryption Specification (RAES).
 * <p>
 * To accomodate the transparent random read access feature, RAES specifies
 * a multistep authentication process:
 * <p>
 * The first step is mandatory and implemented in the constructor of the
 * concrete implementation of this abstract class.
 * For this step only the cipher key and the file length is authenticated,
 * which is fast to process (O(1)).
 * <p>
 * The second step is optional and must be initiated by the client by calling
 * {@link #authenticate}.
 * For this step the entire cipher text is authenticated, which is comparably
 * slow (O(n)).
 * Please note that this step does not require the cipher text to be
 * decrypted first, which features comparably fast processing.
 * <p>
 * So it is up to the application which level of security it needs to
 * provide:
 * Most applications should always call {@code authenticate()} in
 * order to guard against integrity attacks.
 * However, some applications may provide additional (faster) methods for
 * authentication of the pay load, in which case the authentication
 * provided by this class may be safely skipped.
 * <p>
 * Note that this class implements its own virtual file pointer.
 * Thus, if you would like to access the underlying {@code ReadOnlyFile}
 * again after you have finished working with an instance of this class,
 * you should synchronize their file pointers using the pattern as described
 * in the base class {@link DecoratingReadOnlyFile}.
 *
 * @see     RaesOutputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class RaesReadOnlyFile extends CipherReadOnlyFile {

    RaesReadOnlyFile(@CheckForNull ReadOnlyFile rof) {
        super(rof);
    }

    static short readUByte(final byte[] b, final int off) {
        return (short) (b[off] & 0xff);
    }

    static int readUShort(final byte[] b, final int off) {
        return ((b[off + 1] & 0xff) << 8) | (b[off] & 0xff);
    }

    static long readUInt(final byte[] b, int off) {
        off += 3;
        long v = b[off--] & 0xffL;
        v <<= 8;
        v |= b[off--] & 0xffL;
        v <<= 8;
        v |= b[off--] & 0xffL;
        v <<= 8;
        v |= b[off] & 0xffL;
        return v;
    }

    /**
     * Creates a new instance of {@code RaesReadOnlyFile}.
     *
     * @param  file The file to read.
     * @param  param The {@link RaesParameters} required to access the
     *         RAES type actually found in the file.
     *         If the run time class of this parameter does not match the
     *         required parameter interface according to the RAES type found
     *         in the file, but is an instance of the
     *         {@link RaesParametersProvider} interface, it is used to find
     *         the required RAES parameters.
     *         This is applied recursively.
     * @throws NullPointerException If any of the parameters is {@code null}.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws RaesParametersException If no suitable RAES parameters have been
     *         provided or something is wrong with the parameters.
     * @throws RaesException If the file is not RAES compatible.
     * @throws IOException On any other I/O related issue.
     */
    public static RaesReadOnlyFile getInstance(
            final File file,
            final RaesParameters param)
    throws  FileNotFoundException,
            RaesParametersException,
            RaesException,
            IOException {
        final ReadOnlyFile rof = new DefaultReadOnlyFile(file);
        try {
            return getInstance(rof, param);
        } catch (IOException ex) {
            rof.close();
            throw ex;
        }
    }

    /**
     * Returns a new instance of an {@code RaesReadOnlyFile}.
     *
     * @param rof The read only file to read.
     * @param param The {@link RaesParameters} required to access the
     *        RAES type actually found in the file.
     *        If the run time class of this parameter does not match the
     *        required parameter interface according to the RAES type found
     *        in the file, but is an instance of the
     *        {@link RaesParametersProvider} interface, it's queried to find
     *        the required RAES parameters.
     *        This algorithm is recursively applied.
     * @return A new instance of an {@code RaesReadOnlyFile}.
     * @throws NullPointerException If {@code rof} is {@code null}.
     * @throws RaesParametersException If {@code parameters} is {@code null} or
     *         no suitable RAES parameters can be found.
     * @throws RaesException If the file is not RAES compatible.
     * @throws FileNotFoundException If the file cannot get opened for reading.
     * @throws IOException On any other I/O related issue.
     */
    public static RaesReadOnlyFile getInstance(
            final ReadOnlyFile rof,
            RaesParameters param)
    throws IOException {
        /*if (null == parameters)
            throw new NullPointerException();*/

        // Load header data.
        final byte[] leadIn = new byte[LEAD_IN_LENGTH];
        rof.seek(0);
        rof.readFully(leadIn);

        // Check header data.
        if (readUInt(leadIn, 0) != SIGNATURE)
            throw new RaesException("No RAES signature!");
        final int type = readUByte(leadIn, 4);
        switch (type) {
            case 0:
                return new Type0RaesReadOnlyFile(rof,
                        getParameters(Type0RaesParameters.class, param));
            default:
                throw new RaesException("Unknown RAES type: " + type);
        }
    }

    @SuppressWarnings("unchecked")
    private static <P extends RaesParameters> P getParameters(
            final Class<P> type,
            final @CheckForNull RaesParameters param)
    throws RaesParametersException {
        // Order is important here to support multiple interface implementations!
        if (null == param) {
            throw new RaesParametersException("No RAES parameters available!");
        } else if (type.isAssignableFrom(param.getClass())) {
            return (P) param;
        } else if (param instanceof RaesParametersProvider) {
            return getParameters(type,
                    ((RaesParametersProvider) param).get(type));
        } else {
            throw new RaesParametersException();
        }
    }

    /**
     * Returns the key strength which is actually used to decrypt the data
     * of the RAES file.
     */
    public abstract KeyStrength getKeyStrength();

    /**
     * Authenticates all encrypted data in the read only file.
     * It is safe to call this method multiple times to detect if the file
     * has been tampered with meanwhile.
     * <p>
     * This is the second, optional step of authentication.
     * The first, mandatory step is to computeMac the cipher key and
     * cipher text length only and has already been successfully completed
     * in the constructor.
     *
     * @throws RaesAuthenticationException If the computed MAC does not match
     *         the MAC declared in the RAES file.
     * @throws IOException On any I/O related issue.
     */
    public abstract void authenticate()
    throws RaesAuthenticationException, IOException;
}
