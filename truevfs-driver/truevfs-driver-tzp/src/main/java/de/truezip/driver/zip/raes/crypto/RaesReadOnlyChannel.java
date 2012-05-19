/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.crypto;

import static de.truezip.driver.zip.raes.crypto.Constants.HEADER_MIN_LEN;
import static de.truezip.driver.zip.raes.crypto.Constants.SIGNATURE;
import de.truezip.kernel.io.DecoratingReadOnlyChannel;
import de.truezip.kernel.io.PowerBuffer;
import de.truezip.kernel.io.Source;
import de.truezip.key.param.AesKeyStrength;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * This class implements a {@link SeekableByteChannel} for random read-only
 * access to the plain text data of a RAES encrypted file, where RAES means
 * Random Access Encryption Specification.
 * RAES specifies a multistep authentication process:
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
 * Note that this channel implements its own virtual position.
 *
 * @see    RaesOutputStream
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class RaesReadOnlyChannel extends DecoratingReadOnlyChannel {

    /**
     * Creates a new RAES read-only channel.
     *
     * @param  param the {@link RaesParameters} required to access the RAES
     *         type actually found in the file.
     *         If the class of this parameter does not match the required
     *         parameter interface according to the RAES type found in the
     *         file, but is an instance of the {@link RaesParametersProvider}
     *         interface, then it gets queried to find the required RAES
     *         parameters.
     *         This algorithm gets recursively applied.
     * @param  source the source for reading the RAES file from.
     * @return A new RAES read-only channel.
     * @throws RaesParametersException If no RAES parameter can be found which
     *         match the type of RAES file in the given channel.
     * @throws RaesException If the file is not RAES compatible.
     * @throws EOFException on unexpected end-of-file.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public static RaesReadOnlyChannel create(
            final RaesParameters param,
            final Source source)
    throws RaesParametersException, RaesException, EOFException, IOException {
        final SeekableByteChannel channel = source.channel();
        try {
            return create(param, channel);
        } catch (final Throwable ex) {
            try {
                channel.close();
            } catch (final IOException ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    /**
     * Creates a new RAES read-only channel.
     *
     * @param  param the {@link RaesParameters} required to access the RAES
     *         type actually found in the file.
     *         If the class of this parameter does not match the required
     *         parameter interface according to the RAES type found in the
     *         file, but is an instance of the {@link RaesParametersProvider}
     *         interface, then it gets queried to find the required RAES
     *         parameters.
     *         This algorithm gets recursively applied.
     * @param  channel the channel for reading the RAES file from.
     * @return A new RAES read-only channel.
     * @throws RaesParametersException If no RAES parameter can be found which
     *         match the type of RAES file in the given channel.
     * @throws RaesException If the source data is not RAES compatible.
     * @throws EOFException on unexpected end-of-file.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    private static RaesReadOnlyChannel create(
            final RaesParameters param,
            final @WillCloseWhenClosed SeekableByteChannel channel)
    throws RaesParametersException, RaesException, EOFException, IOException {
        final PowerBuffer header = PowerBuffer
                .allocate(HEADER_MIN_LEN)
                .littleEndian()
                .load(channel.position(0));
        if (SIGNATURE != header.getUInt())
            throw new RaesException("No RAES signature!");
        final int type = header.getUByte();
        switch (type) {
            case 0:
                return new Type0RaesReadOnlyChannel(
                        parameters(Type0RaesParameters.class, param),
                        channel);
            default:
                throw new RaesException("Unknown RAES type: " + type);
        }
    }

    private static <P extends RaesParameters> P parameters(
            final Class<P> type,
            @CheckForNull RaesParameters param)
    throws RaesParametersException {
        while (null != param) {
            // Order is important here to support multiple interface implementations!
            if (type.isInstance(param)) {
                return type.cast(param);
            } else if (param instanceof RaesParametersProvider) {
                param = ((RaesParametersProvider) param).get(type);
            } else {
                break;
            }
        }
        throw new RaesParametersException("No suitable RAES parameters available!");
    }

    /**
     * Returns the key strength which is actually used to decrypt the data
     * of the RAES file.
     * 
     * @return The key strength which is actually used to decrypt the data
     *         of the RAES file.
     */
    public abstract AesKeyStrength getKeyStrength();

    /**
     * Authenticates all encrypted data in this read only file.
     * It is safe to call this method multiple times to detect if the file
     * has been tampered with meanwhile.
     * <p>
     * This is the second, optional step of authentication.
     * The first, mandatory step is to compute the cipher key and cipher text
     * length only and must already have been successfully completed in the
     * constructor.
     *
     * @throws RaesAuthenticationException If the computed MAC does not match
     *         the MAC declared in the RAES file.
     * @throws IOException On any I/O related issue.
     */
    public abstract void authenticate()
    throws RaesAuthenticationException, IOException;
}
