/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.raes.crypto;

import net.truevfs.kernel.io.DecoratingOutputStream;
import net.truevfs.kernel.io.Sink;
import net.truevfs.key.param.KeyStrength;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.concurrent.NotThreadSafe;
import org.bouncycastle.crypto.Mac;

/**
 * An {@link OutputStream} which produces a file with data ecnrypted according
 * to the Random Access Encryption Specification (RAES).
 *
 * @see    RaesReadOnlyChannel
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class RaesOutputStream extends DecoratingOutputStream {

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

    /**
     * Creates a new RAES output stream.
     * 
     * @param  param The {@link RaesParameters} used to determine and configure
     *         the type of RAES file created.
     *         If the class of this parameter matches multiple parameter
     *         interfaces, it is at the discretion of this implementation which
     *         one is picked and hence which type of RAES file gets created.
     *         Provide an implementation of the
     *         {@link RaesParametersProvider} interface for more control.
     *         Instances of this interface are queried to find RAES parameters
     *         which match a known RAES type.
     *         This algorithm gets recursively applied.
     * @param  sink the sink for writing the RAES file to.
     * @return A new RAES output stream.
     * @throws RaesParametersException if the RAES parameters type is unknown.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public static RaesOutputStream create(
            final RaesParameters param,
            final Sink sink)
    throws RaesParametersException, IOException {
        RaesParameters p = param;
        while (null != p) {
            // HC SUNT DRACONES!
            if (p instanceof Type0RaesParameters) {
                return new Type0RaesOutputStream((Type0RaesParameters) p,
                        sink);
            } else if (p instanceof RaesParametersProvider) {
                p = ((RaesParametersProvider) p).get(RaesParameters.class);
            } else {
                break;
            }
        }
        throw new RaesParametersException("Unknown RAES parameter type: " + param.getClass());
    }
}
