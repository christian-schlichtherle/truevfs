/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.crypto;

import static de.truezip.kernel.io.Buffers.copy;
import de.truezip.kernel.io.DecoratingReadOnlyChannel;
import de.truezip.kernel.io.Streams;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import static java.lang.Math.min;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import org.bouncycastle.crypto.Mac;

/**
 * A seekable byte channel for transparent random read-only access to the plain
 * text of an encrypted file.
 * The client must call {@link #init(SeekableBlockCipher)}
 * before it can actually read anything!
 * Note that this channel implements its own virtual position.
 *
 * @see    CipherOutputStream
 * @author Christian Schlichtherle
 */
//
// Implementation notes:
//
// In order to provide optimum performance, this class implements a read ahead
// strategy with lazy decryption.
// So the encrypted data of the file is read ahead into an internal buffer in
// order to minimize file access.
// Upon request by the application only, this buffer is then decrypted block by
// block into the buffer provided by the application.
//
// For similar reasons, this class is not a sub-class of
// BufferedReadOnlyChannel though its algorithm and code is pretty similar.
// In fact, this class uses an important performance optimization:
// Whenever possible, encrypted data in the buffer is directly decrypted into
// the user provided buffer.
// If BufferedReadOnlyChannel would be used as the base class instead, we would
// have to provide another buffer to copy the data into before we could
// actually decrypt it, which is redundant.
//
// TODO: Rip out duplicated IntervalReadOnlyChannel functionality!
@NotThreadSafe
public abstract class CipherReadOnlyChannel
extends DecoratingReadOnlyChannel {

    /** The size of the encrypted data in the decorated channel. */
    private long size;

    /** The virtual position of this channel. */
    private long pos;

    /**
     * The position in the decorated channel where the buffer with the
     * encrypted data starts.
     * This is always a multiple of the cipher's block size.
     */
    private long bufferPos;

    /**
     * The buffer to the encrypted data of the decorated channel.
     * Note that this buffer contains <em>encrypted</em> data.
     * The size of the buffer is a multiple of the cipher's block size.
     */
    private ByteBuffer buffer;

    /** The seekable block cipher for random access decryption. */
    private @CheckForNull SeekableBlockCipher cipher;

    /**
     * The position in the decorated channel where the block with the
     * decrypted data starts.
     * This is always a multiple of the cipher's block size.
     */
    private long blockPos;

    /**
     * The block byte buffer to use for the decryption of cipher blocks.
     * Note that this buffer contains <em>decrypted</em> data.
     */
    private ByteBuffer block;

    /**
     * Constructs a new cipher read-only channel.
     * The client must call {@link #init(SeekableBlockCipher)}
     * before it can actually read anything!
     *
     * @param channel A seekable byte channel.
     *        This may be {@code null} now, but is expected to get initialized
     *        <em>before</em> a call to {@link #init}.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    protected CipherReadOnlyChannel(
            @CheckForNull @WillCloseWhenClosed SeekableByteChannel channel) {
        super(channel);
    }

    /**
     * Initializes this cipher read-only channel.
     * This method must get called before the first read access!
     *
     * @param  cipher the seekable block cipher.
     * @throws IllegalStateException if {@link #channel} is {@code null} or
     *         if this object has already been initialized.
     * @throws IllegalArgumentException if {@code cipher} is {@code null} or
     *         if {@code start} or {@code size} are less than zero.
     * @throws IOException on any I/O failure.
     */
    protected final void init(final SeekableBlockCipher cipher)
    throws IOException {
        // Check state.
        if (null == channel)
            throw new IllegalStateException();
        if (null != this.cipher)
            throw new IllegalStateException();

        // Check parameters.
        if (null == (this.cipher = cipher))
            throw new IllegalArgumentException();

        size = channel.size();
        blockPos = size;
        final int blockSize = cipher.getBlockSize();
        block = ByteBuffer.allocate(blockSize);
        invalidateBuffer();
        buffer = ByteBuffer.allocate(Streams.BUFFER_SIZE / blockSize * blockSize); // round down to multiple of block size

        assert 0 == pos;
        assert blockSize == block.limit();
        assert blockSize == block.capacity();
        assert buffer.capacity() % blockSize == 0;
        assert buffer.limit() == buffer.capacity();
    }

    /**
     * Returns the authentication code of the encrypted data in this cipher
     * read-only channel using the given Message Authentication Code (MAC)
     * object.
     * It is safe to call this method multiple times to detect if the file
     * has been tampered with meanwhile.
     *
     * @param  mac a properly initialized MAC object.
     * @return A byte buffer with the authentication code.
     * @throws IOException on any I/O failure.
     */
    protected ByteBuffer computeMac(final Mac mac) throws IOException {
        final int bufferSize = buffer.limit();
        final ByteBuffer buf = ByteBuffer.allocate(mac.getMacSize());
        final long position = position();
        try {
            for (pos = 0; pos < size; pos += bufferSize) {
                positionBuffer();
                final long remaining = size - bufferPos;
                mac.update(buffer.array(), 0, (int) min(bufferSize, remaining));
            }
            final int bufLen = mac.doFinal(buf.array(), 0);
            assert bufLen == buf.limit();
        } finally {
            position(position);
        }
        return buf;
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        // Check no-op first for compatibility with FileChannel.
        final int remaining = dst.remaining();
        if (0 >= remaining)
            return 0;

        // Check is open and not at EOF.
        final long size = size();
        if (pos >= size) // do NOT cache pos!
            return -1;

        // Setup.
        final int blockSize = block.limit();
        int total = 0; // amount of data copied to dst

        {
            // Partial read of block data at the start.
            final int p = (int) (pos % blockSize);
            if (p != 0) {
                // The virtual position is NOT starting on a block boundary.
                positionBlock();
                block.position(p);
                total = copy(block, dst);
                assert total > 0;
                pos += total;
            }
        }

        if (dst.hasArray()) {
            // Full read of block data in the middle.
            final SeekableBlockCipher cipher = this.cipher;
            long blockCounter = pos / blockSize;
            while (total + blockSize < remaining && pos + blockSize < size) {
                // The virtual position is starting on a block boundary.
                positionBuffer();
                cipher.setBlockCounter(blockCounter++);
                final int copied = cipher.processBlock(buffer.array(), (int) (pos - bufferPos), dst.array(), dst.arrayOffset() + total);
                assert copied == blockSize;
                dst.position(dst.position() + copied);
                total += blockSize;
                pos += blockSize;
            }
        }

        // Read of remaining block data.
        while (total < remaining && pos < size) {
            // The virtual position is starting on a block boundary.
            positionBlock();
            block.rewind();
            final int copied = copy(block, dst);
            assert copied > 0;
            total += copied;
            pos += copied;
        }

        // Assert that at least one byte has been read.
        // Note that EOF has been checked before.
        assert 0 < total;
        return total;
    }

    @Override
    public long position() throws IOException {
        checkOpen();
        return pos;
    }

    @Override
    public SeekableByteChannel position(final long pos) throws IOException {
        if (0 > pos)
            throw new IllegalArgumentException();
        checkOpen();
        this.pos = pos;
        return this;
    }

    @Override
    public long size() throws IOException {
        checkOpen();
        return size;
    }

    /**
     * Positions the block with so that it holds the decrypted data
     * referenced by the virtual file pointer.
     *
     * @throws IOException on any I/O failure.
     *         The block is not positioned in this case.
     */
    private void positionBlock() throws IOException {
        assert null != cipher;

        // Check position.
        final long pos = this.pos;
        final int blockSize = block.limit();
        if (blockPos <= pos) {
            final long nextBlockOff = blockPos + blockSize;
            if (pos < nextBlockOff)
                return;
        }

        // Move position.
        positionBuffer();
        final long blockCounter = pos / blockSize;
        blockPos = blockCounter * blockSize;

        // Decrypt block from buffer.
        cipher.setBlockCounter(blockCounter);
        cipher.processBlock(buffer.array(), (int) (blockPos - bufferPos), block.array(), 0);
    }

    /** Triggers a reload of the buffer on the next read access. */
    private void invalidateBuffer() {
        bufferPos = Long.MIN_VALUE;
    }

    /**
     * Positions the buffer so that it holds the encrypted data
     * referenced by the virtual file pointer.
     *
     * @throws IOException on any I/O failure.
     *         The buffer gets invalidated in this case.
     */
    private void positionBuffer() throws IOException {
        // Check position.
        final long pos = this.pos;
        final int bufferSize = buffer.limit();
        final long nextBufferPos = bufferPos + bufferSize;
        if (bufferPos <= pos && pos < nextBufferPos)
            return;

        try {
            // Move position.
            bufferPos = pos / bufferSize * bufferSize; // round down to multiple of buffer size
            if (bufferPos != nextBufferPos)
                channel.position(bufferPos);

            // Fill buffer until end of file or buffer.
            // This should normally complete in one loop cycle, but we do not
            // depend on this as it would be a violation of the contract for a
            // SeekableByteChannel.
            buffer.rewind();
            int total = 0;
            do {
                int read = channel.read(buffer);
                if (0 > read)
                    break;
                total += read;
            } while (total < bufferSize);
        } catch (final Throwable ex) {
            invalidateBuffer();
            throw ex;
        }
    }
}
