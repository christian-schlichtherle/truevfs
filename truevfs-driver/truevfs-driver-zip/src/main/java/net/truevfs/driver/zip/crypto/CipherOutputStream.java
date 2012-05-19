/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.crypto;

import net.truevfs.kernel.io.DecoratingOutputStream;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;

/**
 * Similar to {@code javax.crypto.CipherOutputStream} with some exceptions:
 * <ul>
 * <li>This implementation is based on Bouncy Castle's lightweight crypto API
 *     and uses a {@link BufferedBlockCipher} for ciphering.
 * <li>The {@link #cipher} used for encryption or decryption is accessible to
 *     subclasses.
 * <li>The {@code flush()} method just flushes the underlying output stream
 *     and has no effect on the cipher.
 * <li>A {@link #finish()} method has been added to allow finishing the output
 *     (probably producing padding bytes) without closing the output.
 *     This could be used in a subclass to produce a trailer with additional
 *     information about the ciphered data (e.g. a MAC).
 * </ul>
 *
 * @see    CipherReadOnlyChannel
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class CipherOutputStream extends DecoratingOutputStream {

    /** The buffered block cipher used for processing the output. */
    private @CheckForNull BufferedBlockCipher cipher;

    /**
     * The cipher output buffer used for processing the output
     * to the decorated stream.
     * This buffer is autosized to the largest buffer written to this stream.
     */
    private byte[] buffer = new byte[0];

    /**
     * Creates a new cipher output stream.
     *
     * @param cipher the block cipher.
     * @param out the output stream.
     */
    @CreatesObligation
    public CipherOutputStream(
            final BufferedBlockCipher cipher,
            final @WillCloseWhenClosed OutputStream out) {
        super(Objects.requireNonNull(out));
        this.cipher = Objects.requireNonNull(cipher);
    }

    /**
     * Checks that this cipher output stream is in open state, which requires
     * that {@link #cipher} is not {@code null}.
     *
     * @throws IOException If the preconditions do not hold.
     */
    private BufferedBlockCipher cipher() throws IOException {
        final BufferedBlockCipher cipher = this.cipher;
        if (null == cipher)
            throw new IOException("Cipher output stream has been closed!");
        return cipher;
    }

    /**
     * Ciphers and writes the given byte to the underlying output stream.
     *
     * @param  b The byte to cipher and write.
     * @throws IOException If out or cipher aren't properly initialized,
     *         the stream has been closed or an I/O error occured.
     */
    @Override
    public void write(final int b)
    throws IOException {
        final BufferedBlockCipher cipher = cipher();
        int cipherLen = cipher.getUpdateOutputSize(1);
        byte[] cipherOut = this.buffer;
        if (cipherLen > cipherOut.length)
            this.buffer = cipherOut = new byte[cipherLen];
        cipherLen = cipher.processByte((byte) b, cipherOut, 0);
        if (cipherLen > 0)
            out.write(cipherOut, 0, cipherLen);
    }

    /**
     * Ciphers and writes the contents of the given byte array to the
     * underlying output stream.
     *
     * @param  buf The buffer holding the data to cipher and write.
     * @param  off The start offset of the data in the buffer.
     * @param  len The number of bytes to cipher and write.
     * @throws IOException If out or cipher aren't properly initialized,
     *         the stream has been closed or an I/O error occured.
     */
    @Override
    public void write(final byte[] buf, final int off, final int len)
    throws IOException {
        final BufferedBlockCipher cipher = cipher();
        int cipherLen = cipher.getUpdateOutputSize(len);
        byte[] cipherOut = this.buffer;
        if (cipherLen > cipherOut.length)
            this.buffer = cipherOut = new byte[cipherLen];
        cipherLen = cipher.processBytes(buf, off, len, cipherOut, 0);
        out.write(cipherOut, 0, cipherLen);
    }

    /**
     * Finishes and voids this cipher output stream.
     * Calling this method causes all remaining buffered bytes to get written
     * and padded if necessary.
     * Afterwards, this stream will behave as if it had been closed, although
     * the decorated stream may still be open.
     *
     * @throws IOException If {@code out} or {@code cipher} aren't properly
     *         initialized, an I/O error occurs or the cipher
     *         text is invalid because some required padding is missing.
     */
    public void finish() throws IOException {
        final BufferedBlockCipher cipher = this.cipher;
        if (null == cipher)
            return;
        this.cipher = null;

        int cipherLen = cipher.getOutputSize(0);
        byte[] cipherOut = this.buffer;
        if (cipherLen > cipherOut.length)
            this.buffer = cipherOut = new byte[cipherLen];
        try {
            cipherLen = cipher.doFinal(cipherOut, 0);
        } catch (InvalidCipherTextException ex) {
            throw new IOException(ex);
        }
        out.write(cipherOut, 0, cipherLen);
    }

    @Override
    public void close() throws IOException {
        finish();
        out.close();
    }
}
