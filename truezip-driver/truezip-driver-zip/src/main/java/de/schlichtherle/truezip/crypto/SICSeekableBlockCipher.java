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
package de.schlichtherle.truezip.crypto;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Implements Counter (CTR) mode (alias Segmented Integer Counter - SIC).
 * on top of a simple block cipher.
 * This code is based on bouncy castle's {@link SICBlockCipher} class,
 * but also allows random access to a block.
 *
 * @author  The Legion of the Bouncy Castle (majority of the code)
 * @author  Christian Schlichtherle (optimizations and extension to support seeking)
 * @version $Id$
 */
public class SICSeekableBlockCipher implements SeekableBlockCipher {

    private final BlockCipher cipher;
    private long blockCounter;
    private final int blockSize;
    private final byte[] IV;
    private final byte[] cipherIn;
    private final byte[] cipherOut;

    /**
     * Constructs a new SIC seekable block cipher mode.
     *
     * @param cipher The underlying block cipher to use.
     */
    public SICSeekableBlockCipher(final BlockCipher cipher) {
        this.cipher = cipher;
        this.blockSize = cipher.getBlockSize();
        this.IV = new byte[blockSize];
        this.cipherIn = new byte[blockSize];
        this.cipherOut = new byte[blockSize];
    }

    /**
     * Returns the underlying block cipher which we are decorating.
     *
     * @return The underlying block cipher which we are decorating.
     */
    public BlockCipher getUnderlyingCipher() {
        return cipher;
    }

    @Override
    public void init(
            boolean forEncryption, // not used for CTR mode
            CipherParameters params) {
        ParametersWithIV ivParams = (ParametersWithIV) params;
        byte[] iv = ivParams.getIV();
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
    public int processBlock(
            final byte[] in,
            int inOff,
            final byte[] out,
            int outOff)
    throws DataLengthException, IllegalStateException {
        updateCounter();
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

        blockCounter++;

        return blockSize;
    }

    private void updateCounter() {
        // This is big endian!
        long blockCounter = this.blockCounter;
        for (int i = blockSize; --i >= 0; ) {
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
        return blockCounter;
    }

    @Override
    public void reset() {
        blockCounter = 0;
        cipher.reset();
    }
}