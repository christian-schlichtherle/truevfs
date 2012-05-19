/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.crypto;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Implements Counter (CTR) mode (alias Segmented Integer Counter - SIC)
 * on top of a simple block cipher.
 * This code is based on bouncy castle's {@link SICBlockCipher} class,
 * but allows random access to a block, too.
 * Like the {@link SICBlockCipher} class, the block counter is incremented
 * <em>after</em> updating the cipher input in <em>big endian</em> order.
 *
 * @author The Legion of the Bouncy Castle (majority of the code)
 * @author Christian Schlichtherle (optimizations and extension to support seeking)
 */
public class CtrBlockCipher implements SeekableBlockCipher {

    protected final BlockCipher cipher;
    protected final int blockSize;
    protected long blockCounter;
    protected final byte[] IV;
    protected final byte[] cipherIn;
    protected final byte[] cipherOut;

    /**
     * Constructs a new CTR block cipher.
     *
     * @param cipher The underlying block cipher to use.
     */
    public CtrBlockCipher(final BlockCipher cipher) {
        this.cipher = cipher;
        this.blockSize = cipher.getBlockSize();
        this.IV = new byte[blockSize];
        this.cipherIn = new byte[blockSize];
        this.cipherOut = new byte[blockSize];
    }

    @Override
    public void init(
            final boolean forEncryption, // not used for CTR mode
            final CipherParameters params) {
        final ParametersWithIV ivParams = (ParametersWithIV) params;
        final byte[] iv = ivParams.getIV();
        System.arraycopy(iv, 0, IV, 0, IV.length);
        reset();
        cipher.init(true, ivParams.getParameters());
    }

    @Override
    public String getAlgorithmName() {
        // Must add "/SIC" in order to make decorating BufferedBlockCipher work
        // correctly.
        return cipher.getAlgorithmName() + "/SIC";
    }

    @Override
    public int getBlockSize() {
        assert blockSize == cipher.getBlockSize();
        return blockSize;
    }

    @Override
    public final int processBlock(
            final byte[] in,
            int inOff,
            final byte[] out,
            int outOff)
    throws DataLengthException, IllegalStateException {
        incCounter();
        cipher.processBlock(cipherIn, 0, cipherOut, 0);

        // XOR the cipherOut with the plaintext producing the cipher text.
        final int blockSize = this.blockSize;
        {
            int i = blockSize;
            inOff += i;
            outOff += i;
            while (i > 0)
                out[--outOff] = (byte) (in[--inOff] ^ cipherOut[--i]);
        }

        return blockSize;
    }

    protected void incCounter() {
        final int blockSize = this.blockSize;
        long blockCounter = this.blockCounter++; // post-increment the block counter!
        for (int i = blockSize; --i >= 0; ) { // big endian order!
            blockCounter += IV[i] & 0xff;
            cipherIn[i] = (byte) blockCounter;
            blockCounter >>>= 8;
        }
    }

    @Override
    public void setBlockCounter(final long blockCounter) {
        this.blockCounter = blockCounter;
    }

    @Override
    public long getBlockCounter() {
        return this.blockCounter;
    }

    @Override
    public void reset() {
        cipher.reset();
        blockCounter = 0;
    }
}
