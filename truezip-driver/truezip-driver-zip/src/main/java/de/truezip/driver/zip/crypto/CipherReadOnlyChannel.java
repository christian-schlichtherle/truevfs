/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.crypto;

import de.truezip.kernel.io.DecoratingReadOnlyChannel;
import de.truezip.kernel.io.Streams;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import static java.lang.Math.min;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import org.bouncycastle.crypto.Mac;

/**
 * Provides buffered random read-only access to the plain text of an encrypted
 * file.
 * Note that this channel implements its own virtual position.
 *
 * @see    CipherOutputStream
 * @author Christian Schlichtherle
 */
//
// Note that this is mostly a copy of
// de.truezip.kernel.io.BufferedReadOnlyChannel which has been tuned for
// performance.
//
@NotThreadSafe
public final class CipherReadOnlyChannel extends DecoratingReadOnlyChannel {

    private static final long INVALID = Long.MIN_VALUE;

    /** The seekable block cipher for random access decryption. */
    private final SeekableBlockCipher cipher;

    /** The virtual position of this channel. */
    private long pos;

    /**
     * The position in the decorated channel where the buffer with the
     * encrypted data starts.
     * This is always a multiple of the cipher's block size.
     */
    private long bufferStart = INVALID;

    /**
     * The buffer for the encrypted channel data.
     * The size of the buffer is a multiple of the cipher's block size.
     */
    private byte[] buffer;

    /**
     * The position in the decorated channel where the block with the
     * decrypted data starts.
     * This is always a multiple of the cipher's block size.
     */
    private long blockStart = INVALID;

    /** The buffer for the decrypted channel data. */
    private byte[] block;

    /**
     * Constructs a new cipher read-only channel.
     *
     * @param cipher the seekable block cipher.
     * @param channel the seekable byte channel.
     */
    @CreatesObligation
    public CipherReadOnlyChannel(
            final SeekableBlockCipher cipher,
            final @WillCloseWhenClosed SeekableByteChannel channel) {
        this(cipher, channel, Streams.BUFFER_SIZE);
    }

    /**
     * Constructs a new cipher read-only channel.
     *
     * @param cipher the seekable block cipher.
     * @param channel the seekable byte channel.
     * @param bufferSize the size of the byte buffer.
     *        The value gets rounded down to a multiple of the cipher's
     *        blocksize or the cipher's blocksize, whatever is larger.
     */
    @CreatesObligation
    public CipherReadOnlyChannel(
            final SeekableBlockCipher cipher,
            final @WillCloseWhenClosed SeekableByteChannel channel,
            int bufferSize) {
        super(channel);
        if (null == channel)
            throw new NullPointerException();
        if (null == (this.cipher = cipher))
            throw new NullPointerException();
        final int blockSize = cipher.getBlockSize();
        block = new byte[blockSize];
        if (bufferSize < blockSize)
            bufferSize = blockSize;
        buffer = new byte[bufferSize / blockSize * blockSize]; // round down to multiple of block size
        assert buffer.length % blockSize == 0;
    }

    /**
     * Returns the authentication code of the encrypted data in this cipher
     * read-only channel using the given Message Authentication Code (MAC)
     * object.
     * It is safe to call this method multiple times to detect if the file
     * has been tampered with meanwhile.
     *
     * @param  mac a properly initialized MAC object.
     * @return A byte array with the authentication code.
     * @throws IOException on any I/O error.
     */
    public byte[] mac(final Mac mac) throws IOException {
        final long position = position();
        try {
            final long size = size();
            final int bufferSize = buffer.length;
            for (pos = 0; pos < size; ) {
                positionBuffer();
                final int bufferLimit = (int) min(bufferSize, size - bufferStart);
                assert 0 < bufferLimit;
                mac.update(buffer, 0, bufferLimit);
                pos += bufferLimit;
            }
            final byte[] buf = new byte[mac.getMacSize()];
            final int bufLength = mac.doFinal(buf, 0);
            assert bufLength == buf.length;
            return buf;
        } finally {
            position(position);
        }
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        // Check no-op first for compatibility with FileChannel.
        final int remaining = dst.remaining();
        if (remaining <= 0)
            return 0;

        // Check is open and not at EOF.
        final long size = size();
        if (position() >= size)
            return -1;

        // Setup.
        int total = 0; // amount of data copied to dst
        final int blockSize = block.length;

        // Partial read of block data at the start.
        positionBlock();
        if (pos != blockStart) {
            assert pos % blockSize != 0;
            final int blockPos = (int) (pos - blockStart);
            int blockLimit = min(remaining, blockSize - blockPos);
            blockLimit = (int) min(blockLimit, size - pos);
            assert blockLimit > 0;
            dst.put(block, blockPos, blockLimit);
            total += blockLimit;
            pos += blockLimit;
        }

        if (total < remaining && pos < size && dst.hasArray()) {
            // Full read of block data in the middle.
            final SeekableBlockCipher cipher = this.cipher;
            final byte[] buffer = this.buffer;
            final byte[] dstArray = dst.array();
            final int dstArrayOffset = dst.arrayOffset();
            int dstPosition = dst.position();
            final int dstCapacity = dst.capacity();
            long blockCounter = pos / blockSize;
            while (total + blockSize <= remaining
                    && dstPosition + blockSize <= dstCapacity
                    && pos + blockSize <= size) {
                assert pos % blockSize == 0;
                positionBuffer();
                cipher.setBlockCounter(blockCounter++);
                final int bufferOff = (int) (pos - bufferStart);
                final int blockLimit = cipher.processBlock(
                        buffer, bufferOff,
                        dstArray, dstArrayOffset + dstPosition);
                assert blockLimit == blockSize;
                dst.position(dstPosition += blockLimit);
                total += blockLimit;
                pos += blockLimit;
            }
        }

        // Read of remaining block data.
        while (total < remaining && pos < size) {
            assert pos % blockSize == 0;
            positionBlock();
            final int blockPos = (int) (pos - blockStart);
            int blockLimit = min(remaining - total, blockSize - blockPos);
            blockLimit = (int) min(blockLimit, size - pos);
            assert blockLimit > 0;
            dst.put(block, blockPos, blockLimit);
            total += blockLimit;
            pos += blockLimit;
        }

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

    /**
     * Positions the block with so that it holds the decrypted data
     * referenced by the virtual file pointer.
     *
     * @throws IOException on any I/O error.
     *         The block is not positioned in this case.
     */
    private void positionBlock() throws IOException {
        final byte[] block = this.block;
        final int blockSize = block.length;

        // Check position.
        final long pos = this.pos;
        long blockStart = this.blockStart;
        if (blockStart <= pos) {
            final long nextBlockStart = blockStart + blockSize;
            if (pos < nextBlockStart)
                return;
        }

        // Move position.
        final SeekableBlockCipher cipher = this.cipher;
        assert null != cipher;
        positionBuffer();
        final long blockCounter = pos / blockSize;
        cipher.setBlockCounter(blockCounter);
        this.blockStart = blockStart = blockCounter * blockSize;

        // Decrypt block from buffer.
        final int bufferPos = (int) (blockStart - bufferStart);
        final int processed = cipher.processBlock(
                buffer, bufferPos,
                block, 0);
        assert processed == blockSize;
    }

    /**
     * Positions the buffer so that it holds the encrypted data
     * referenced by the virtual channel pointer.
     *
     * @throws IOException on any I/O error.
     *         The buffer gets invalidated in this case.
     */
    private void positionBuffer() throws IOException {
        final int bufferSize = buffer.length;

        // Check position.
        final long pos = this.pos;
        long bufferStart = this.bufferStart;
        final long nextBufferStart = bufferStart + bufferSize;
        if (bufferStart <= pos && pos < nextBufferStart)
            return;

        try {
            final SeekableByteChannel channel = this.channel;

            // Move position.
            // Round down to multiple of buffer size.
            this.bufferStart = bufferStart = pos / bufferSize * bufferSize;
            if (bufferStart != nextBufferStart)
                channel.position(bufferStart);

            // Fill buffer until end of file or buffer.
            // This should normally complete in one loop cycle, but we do not
            // depend on this as it would be a violation of ReadOnlyFile's
            // contract.
            int total = 0;
            final ByteBuffer buffer = ByteBuffer.wrap(this.buffer);
            do {
                int read = channel.read(buffer);
                if (read < 0)
                    break;
                total += read;
            } while (total < bufferSize);
        } catch (final IOException ex) {
            this.bufferStart = INVALID;
            throw ex;
        }
    }
}
