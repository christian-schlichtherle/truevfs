/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.crypto;

import de.truezip.kernel.io.Streams;
import de.truezip.kernel.rof.DecoratingReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFile;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import static java.lang.Math.min;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import org.bouncycastle.crypto.Mac;

/**
 * A read only file for transparent random read access to an encrypted file.
 * The client must call {@link #init(SeekableBlockCipher, long, long)}
 * before it can actually read anything!
 * <p>
 * Note that this class implements its own virtual file pointer.
 * Thus, if you would like to access the underlying {@code ReadOnlyFile}
 * again after you have finished working with an instance of this class,
 * you should synchronize their file pointers using the pattern as described
 * in the base class {@link DecoratingReadOnlyFile}.
 *
 * @see    CipherOutputStream
 * @author Christian Schlichtherle
 */
//
// Implementation notes:
//
// In order to provide optimum performance, this class implements a read ahead
// strategy with lazy decryption.
// So the encrypted data of the file is read ahead into an internal window
// buffer in order to minimize file access.
// Upon request by the application only, this buffer is then decrypted block by
// block into the buffer provided by the application.
//
// For similar reasons, this class is NOT a subclass of
// BufferedReadOnlyFile though their algorithm and code is pretty similar.
// In fact, this class uses an important performance optimization:
// Whenever possible, encrypted data in the window buffer is directly
// decrypted into the user provided buffer.
// If BufferedReadOnlyFile would be used as the base class instead, we would
// have to provide another buffer to copy the data into before we could
// actually decrypt it, which is redundant.
//
@NotThreadSafe
public abstract class CipherReadOnlyFile extends DecoratingReadOnlyFile {

    /** Start offset of the encrypted data. */
    private long start;

    /** The length of the encrypted data. */
    private long length;

    /**
     * The virtual file pointer in the encrypted data.
     * This is relative to the start.
     */
    private long fp;

    /**
     * The current offset in the encrypted file where the buffer window starts.
     * This is always a multiple of the block size.
     */
    private long windowOff;

    /**
     * The buffer window to the encrypted file.
     * Note that this buffer contains encrypted data only.
     * The actual size of the window is a multiple of the cipher's block size
     * and may be slightly smaller than {@link #MAX_WINDOW_LEN}.
     */
    private byte[] window;

    /** The seekable block cipher which allows random access. */
    private @CheckForNull SeekableBlockCipher cipher;

    /**
     * The current offset in the encrypted file where the data starts that
     * has been decrypted to the block.
     * This is always a multiple of the block size.
     */
    private long blockOff;

    /**
     * The block buffer to use for decryption of partial blocks.
     * Note that this buffer contains decrypted data only.
     */
    private byte[] block;

    /**
     * Creates a read only file for transparent random read access to an
     * encrypted file.
     * The client must call {@link #init(SeekableBlockCipher, long, long)}
     * before it can actually read anything!
     *
     * @param rof A read-only file.
     *        This may be {@code null}, but must be properly initialized
     *        <em>before</em> a call to {@link #init}.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    protected CipherReadOnlyFile(@Nullable @WillCloseWhenClosed ReadOnlyFile rof) {
        super(rof);
    }

    /**
     * Checks that this cipher read only file is in open state, which requires
     * that {@link #cipher} is not {@code null}.
     *
     * @throws IOException If the preconditions do not hold.
     */
    private SeekableBlockCipher cipher() throws IOException {
        final SeekableBlockCipher cipher = this.cipher;
        if (null == cipher)
            throw new IOException("Cipher read only file has been closed!");
        return cipher;
    }

    /**
     * Initializes this cipher read only file - must be called before first
     * read access!
     *
     * @param  start The start offset of the encrypted data in this file.
     * @param  length The length of the encrypted data in this file.
     * @throws IOException If this read only file has already been closed.
     *         This exception is <em>not</em> recoverable.
     * @throws IllegalStateException if {@link #rof} is {@code null} or
     *         if this object has already been initialized.
     * @throws IllegalArgumentException if {@code cipher} is {@code null} or
     *         if {@code start} or {@code length} are less than zero.
     */
    protected final void init(
            final SeekableBlockCipher cipher,
            final long start,
            final long length) {
        // Check state.
        if (null == this.rof)
            throw new IllegalStateException();
        if (null != this.cipher)
            throw new IllegalStateException();

        // Check parameters.
        if (null == (this.cipher = cipher))
            throw new IllegalArgumentException();
        if (0 > (this.start = start))
            throw new IllegalArgumentException();
        if (0 > (this.length = length))
            throw new IllegalArgumentException();

        this.blockOff = length;
        final int blockLen = cipher.getBlockSize();
        this.block = new byte[blockLen];
        this.windowOff = Long.MIN_VALUE; // invalidate window
        this.window = new byte[(Streams.BUFFER_SIZE / blockLen) * blockLen]; // round down to multiple of block size

        assert this.fp == 0;
        assert this.block.length > 0;
        assert this.window.length > 0;
        assert this.window.length % this.block.length == 0;
    }

    /**
     * Returns the authentication code of the encrypted data in this cipher
     * read only file using the given Message Authentication Code (MAC) object.
     * It is safe to call this method multiple times to detect if the file
     * has been tampered with meanwhile.
     *
     * @param mac A properly initialized MAC object.
     *
     * @throws IOException On any I/O related issue.
     */
    protected byte[] computeMac(final Mac mac) throws IOException {
        final int windowLen = window.length;
        final byte[] buf = new byte[mac.getMacSize()];
        final long safedFp = getFilePointer();
        try {
            for (fp = 0; fp < length; fp += windowLen) {
                positionWindow();
                final long remaining = length - windowOff;
                mac.update(window, 0, (int) min(windowLen, remaining));
            }
            final int bufLen = mac.doFinal(buf, 0);
            assert bufLen == buf.length;
        } finally {
            seek(safedFp);
        }
        return buf;
    }

    @Override
    public long length() throws IOException {
        cipher();
        return length;
    }

    @Override
    public long getFilePointer() throws IOException {
        cipher();
        return fp;
    }

    @Override
    public void seek(final long fp) throws IOException {
        cipher();
        if (fp < 0)
            throw new IOException("file pointer must not be negative");
        if (fp > length)
            throw new IOException("file pointer (" + fp
                    + ") is larger than file length (" + length + ")");

        this.fp = fp;
    }

    @Override
    public int read() throws IOException {
        // Check state.
        cipher();
        if (fp >= length)
            return -1;

        // Position block and return its decrypted data.
        positionBlock();
        return block[(int) (fp++ % block.length)] & 0xff;
    }

    @Override
    public int read(final byte[] buf, final int off, final int len)
    throws IOException {
        // Check no-op first for compatibility with RandomAccessFile.
        if (0 >= len)
            return 0;

        final SeekableBlockCipher cipher = cipher(); // check state.
        if (fp >= length)
            return -1;

        // Check parameters.
        {
            final int offPlusLen = off + len;
            if ((off | len | offPlusLen | buf.length - offPlusLen) < 0)
                throw new IndexOutOfBoundsException();
        }

        // Setup.
        final int blockLen = block.length;
        int read = 0; // amount of decrypted data copied to buf

        {
            // Partial read of block data at the start.
            final int o = (int) (fp % blockLen);
            if (o != 0) {
                // The virtual file pointer is NOT starting on a block boundary.
                positionBlock();
                read = min(len, blockLen - o);
                read = (int) min(read, length - fp);
                System.arraycopy(block, o, buf, off, read);
                fp += read;
            }
        }

        {
            // Full read of block data in the middle.
            long blockCounter = fp / blockLen;
            while (read + blockLen < len && fp + blockLen < length) {
                // The virtual file pointer is starting on a block boundary.
                positionWindow();
                cipher.setBlockCounter(blockCounter++);
                cipher.processBlock(window, (int) (fp - windowOff), buf, off + read);
                read += blockLen;
                fp += blockLen;
            }
        }

        // Partial read of block data at the end.
        if (read < len && fp < length) {
            // The virtual file pointer is starting on a block boundary.
            positionBlock();
            final int n = (int) min(len - read, length - fp);
            System.arraycopy(block, 0, buf, off + read, n);
            read += n;
            fp += n;
        }

        // Assert that at least one byte has been read if len isn't zero.
        // Note that EOF has been tested before.
        assert read > 0;
        return read;
    }

    /**
     * Closes this read only file and releases any resources associated with it.
     * This method invalidates the state of this object, causing any subsequent
     * calls to a public method to fail with an {@link IOException}.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        cipher = null;
        rof.close();
    }

    /**
     * Positions the block with the decrypted data for partial reading so that
     * it contains the current virtual file pointer in the encrypted file.
     *
     * @throws IOException On any I/O related issue.
     *         The block is not moved in this case.
     */
    private void positionBlock() throws IOException {
        assert null != cipher;

        // Check block position.
        final long fp = this.fp;
        final int blockLen = block.length;
        if (blockOff <= fp) {
            final long nextBlockOff = blockOff + blockLen;
            if (fp < nextBlockOff)
                return;
        }

        // Move block.
        positionWindow();
        final long blockCounter = fp / blockLen;
        blockOff = blockCounter * blockLen;

        // Decrypt block from window.
        cipher.setBlockCounter(blockCounter);
        cipher.processBlock(window, (int) (blockOff - windowOff), block, 0);
    }

    /**
     * Positions the window so that the block containing
     * the current virtual file pointer in the encrypted file is entirely
     * contained in it.
     *
     * @throws IOException On any I/O related issue.
     *         The window is invalidated in this case.
     */
    private void positionWindow() throws IOException {
        // Check window position.
        final long fp = this.fp;
        final int windowLen = window.length;
        final long nextWindowOff = windowOff + windowLen;
        if (windowOff <= fp && fp < nextWindowOff)
            return;

        try {
            // Move window in the encrypted file.
            final int blockLen = block.length;
            windowOff = fp / windowLen * windowLen; // round down to multiple of window size
            if (windowOff != nextWindowOff)
                rof.seek(windowOff + start);

            // Fill window until end of file or buffer.
            // This should normally complete in one loop cycle, but we do not
            // depend on this as it would be a violation of ReadOnlyFile's
            // contract.
            int n = 0;
            do {
                int read = rof.read(window, n, windowLen - n);
                if (0 > read)
                    break;
                n += read;
            } while (n < windowLen);
        } catch (final Throwable ex) {
            windowOff = -windowLen - 1; // force seek() at next positionWindow()
            throw ex;
        }
    }
}