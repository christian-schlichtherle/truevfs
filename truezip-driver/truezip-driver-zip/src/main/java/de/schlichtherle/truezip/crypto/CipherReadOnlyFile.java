/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto;

import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import static java.lang.Math.min;
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
 * @see     CipherOutputStream
 * @author  Christian Schlichtherle
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

    private static final long INVALID = Long.MIN_VALUE;

    /** Start offset of the encrypted data. */
    private long start;

    /** The length of the encrypted data. */
    private long length;

    /** The virtual file pointer in the encrypted data. */
    private long pos;

    /**
     * The current offset in the encrypted file where the buffer window starts.
     * This is always a multiple of the block size.
     */
    private long bufferPos = INVALID;

    /**
     * The buffer for the encrypted file data.
     * Note that this buffer contains encrypted data only.
     * The actual size of the window is a multiple of the cipher's block size
     * and may be slightly smaller than {@link #MAX_WINDOW_LEN}.
     */
    private byte[] buffer;

    /** The seekable block cipher which allows random access. */
    private @Nullable SeekableBlockCipher cipher;

    /**
     * The current offset in the encrypted file where the data starts that
     * has been decrypted to the block.
     * This is always a multiple of the block size.
     */
    private long blockPos = INVALID;

    /** The buffer for the decrypted file data. */
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
     * Initializes this cipher read only file - must be called before first
     * read access!
     *
     * @param  cipher the seekable block cipher.
     * @param  start the start offset of the encrypted data in this file.
     * @param  length the length of the encrypted data in this file.
     * @throws IOException If this read only file has already been closed.
     *         This exception is <em>not</em> recoverable.
     * @throws IllegalStateException if {@link #delegate} is {@code null} or
     *         if this object has already been initialized.
     * @throws IllegalArgumentException if {@code cipher} is {@code null} or
     *         if {@code start} or {@code length} are less than zero.
     */
    protected final void init(
            final SeekableBlockCipher cipher,
            final long start,
            final long length)
    throws IOException {
        // Check state.
        if (null == this.delegate)
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

        final int blockSize = cipher.getBlockSize();
        this.block = new byte[blockSize];
        this.buffer = new byte[(Streams.BUFFER_SIZE / blockSize) * blockSize]; // round down to multiple of block size

        assert this.buffer.length % blockSize == 0;
    }

    /**
     * Asserts that this cipher output stream is in open state, which requires
     * that {@link #cipher} is not {@code null}.
     *
     * @throws IOException If the preconditions do not hold.
     */
    private void checkOpen() throws IOException {
        if (null == cipher)
            throw new IOException("cipher read only file is not in open state");
    }

    /**
     * Returns the authentication code of the encrypted data in this cipher
     * read-only file using the given Message Authentication Code (MAC) object.
     * It is safe to call this method multiple times to detect if the file
     * has been tampered with meanwhile.
     *
     * @param  mac a properly initialized MAC object.
     * @return A byte array with the authentication code.
     * @throws IOException on any I/O error.
     */
    protected byte[] computeMac(final Mac mac) throws IOException {
        final long safedFp = getFilePointer();
        try {
            final long length = this.length;
            final int windowLen = buffer.length;
            for (pos = 0; pos < length; ) {
                positionBuffer();
                final int windowLimit = (int) min(windowLen, length - bufferPos);
                assert 0 < windowLimit;
                mac.update(buffer, 0, windowLimit);
                pos += windowLimit;
            }
            final byte[] buf = new byte[mac.getMacSize()];
            final int bufLength = mac.doFinal(buf, 0);
            assert bufLength == buf.length;
            return buf;
        } finally {
            seek(safedFp);
        }
    }

    @Override
    public int read() throws IOException {
        // Check state.
        checkOpen();
        if (pos >= length)
            return -1;

        // Position block and return its decrypted data.
        positionBlock();
        return block[(int) (pos++ % block.length)] & 0xff;
    }

    @Override
    public int read(final byte[] dst, final int offset, final int remaining)
    throws IOException {
        // Check no-op first for compatibility with RandomAccessFile.
        if (remaining <= 0)
            return 0;

        // Check is open and not at EOF.
        final long length = this.length;
        if (getFilePointer() >= length) // ensure pos is initialized, but do NOT cache!
            return -1;

        // Check parameters.
        {
            final int or = offset + remaining;
            if ((offset | remaining | or | dst.length - or) < 0)
                throw new IndexOutOfBoundsException();
        }

        // Setup.
        int total = 0; // amount of data copied to buf
        final int blockSize = block.length;

        // Partial read of block data at the start.
        positionBlock();
        if (pos != blockPos) {
            assert pos % blockSize != 0;
            final int blockOff = (int) (pos - blockPos);
            total = min(remaining, blockSize - blockOff);
            total = (int) min(total, length - pos);
            assert total > 0;
            System.arraycopy(block, blockOff, dst, offset, total);
            pos += total;
        }

        if (total < remaining && pos < length) {
            // Full read of block data in the middle.
            assert pos % blockSize == 0;
            final SeekableBlockCipher cipher = this.cipher;
            final byte[] buffer = this.buffer;
            long blockCounter = pos / blockSize;
            while (total + blockSize <= remaining && pos + blockSize <= length) {
                // The virtual file pointer is starting on a block boundary.
                positionBuffer();
                cipher.setBlockCounter(blockCounter++);
                final int bufferOff = (int) (pos - bufferPos);
                final int processed = cipher.processBlock(
                        buffer, bufferOff,
                        dst, offset + total);
                assert processed == blockSize;
                total += processed;
                pos += processed;
            }
        }

        if (total < remaining && pos < length) {
            // Partial read of block data at the end.
            assert pos % blockSize == 0;
            positionBlock();
            final int blockOff = (int) (pos - blockPos);
            int processed = min(remaining - total, blockSize - blockOff);
            processed = (int) min(processed, length - pos);
            assert processed > 0;
            System.arraycopy(block, blockOff, dst, offset + total, processed);
            total += processed;
            pos += processed;
        }

        return total;
    }

    @Override
    public long getFilePointer() throws IOException {
        checkOpen();
        return pos;
    }

    @Override
    public void seek(final long pos) throws IOException {
        checkOpen();
        if (pos < 0)
            throw new IOException("file pointer must not be negative");
        if (pos > length)
            throw new IOException("file pointer (" + pos
                    + ") is larger than file length (" + length + ")");
        this.pos = pos;
    }

    @Override
    public long length() throws IOException {
        checkOpen();
        return length;
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
        delegate.close();
    }

    /**
     * Positions the block with the decrypted data for partial reading so that
     * it contains the current virtual file pointer in the encrypted file.
     *
     * @throws IOException on any I/O error.
     *         The block is not moved in this case.
     */
    private void positionBlock() throws IOException {
        final byte[] block = this.block;
        final SeekableBlockCipher cipher = this.cipher;
        assert null != cipher;

        // Check position.
        final long pos = this.pos;
        long blockPos = this.blockPos;
        final int blockSize = block.length;
        if (blockPos <= pos) {
            final long nextBlockPos = blockPos + blockSize;
            if (pos < nextBlockPos)
                return;
        }

        // Move position.
        positionBuffer();
        final long blockCounter = pos / blockSize;
        cipher.setBlockCounter(blockCounter);
        this.blockPos = blockPos = blockCounter * blockSize;

        // Decrypt block from window.
        final int bufferOff = (int) (blockPos - bufferPos);
        final int processed = cipher.processBlock(
                buffer, bufferOff,
                block, 0);
        assert processed == blockSize;
    }

    /**
     * Positions the window so that the block containing
     * the current virtual file pointer in the encrypted file is entirely
     * contained in it.
     *
     * @throws IOException on any I/O error.
     *         The buffer gets invalidated in this case.
     */
    private void positionBuffer() throws IOException {
        final byte[] buffer = this.buffer;
        final int bufferSize = buffer.length;

        // Check position.
        final long pos = this.pos;
        long bufferPos = this.bufferPos;
        final long nextBufferPos = bufferPos + bufferSize;
        if (bufferPos <= pos && pos < nextBufferPos)
            return;

        try {
            final ReadOnlyFile delegate = this.delegate;

            // Move position.
            // Round down to multiple of buffer size.
            this.bufferPos = bufferPos = pos / bufferSize * bufferSize;
            if (bufferPos != nextBufferPos)
                delegate.seek(start + bufferPos);

            // Fill buffer until end of file or buffer.
            // This should normally complete in one loop cycle, but we do not
            // depend on this as it would be a violation of ReadOnlyFile's
            // contract.
            int total = 0;
            do {
                int read = delegate.read(buffer, total, bufferSize - total);
                if (read < 0)
                    break;
                total += read;
            } while (total < bufferSize);
        } catch (final IOException ex) {
            this.bufferPos = INVALID;
            throw ex;
        }
    }
}
