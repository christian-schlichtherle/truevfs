/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.crypto.mode;

import de.schlichtherle.truezip.crypto.SeekableBlockCipher;
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
 * @author The Legion of the Bouncy Castle (majority of the code)
 * @author Christian Schlichtherle (optimizations and extension to support seeking)
 * @version $Id$
 */
public class SICSeekableBlockCipher implements SeekableBlockCipher {

    private final BlockCipher cipher;
    private final int blockSize;
    private final byte[] IV;
    private final byte[] counterIn;
    private long blockCounter; // the blockCounter counter
    private final byte[] counterOut;

    /**
     * Basic constructor.
     *
     * @param cipher The block cipher to be used.
     */
    public SICSeekableBlockCipher(final BlockCipher cipher) {
        this.cipher = cipher;
        this.blockSize = cipher.getBlockSize();
        this.IV = new byte[blockSize];
        this.counterIn = new byte[blockSize];
        this.counterOut = new byte[blockSize];
    }

    /**
     * Returns the underlying block cipher which we are decorating.
     *
     * @return The underlying block cipher which we are decorating.
     */
    public BlockCipher getUnderlyingCipher() {
        return cipher;
    }

    public void init(boolean forEncryption, CipherParameters params)
    throws IllegalArgumentException {
        if (params instanceof ParametersWithIV) {
          ParametersWithIV ivParams = (ParametersWithIV) params;
          byte[] iv = ivParams.getIV();
          System.arraycopy(iv, 0, IV, 0, IV.length);

          reset();
          cipher.init(true, ivParams.getParameters());
        }
    }

    public String getAlgorithmName() {
        // Must add "/SIC" in order to make BufferedBlockCipher work correctly.
        return cipher.getAlgorithmName() + "/SIC";
    }

    public int getBlockSize() {
        assert blockSize == cipher.getBlockSize();
        return blockSize;
    }

    public int processBlock(
            final byte[] in,
            final int inOff,
            final byte[] out,
            final int outOff)
    throws DataLengthException, IllegalStateException {
        updateCounter();
        cipher.processBlock(counterIn, 0, counterOut, 0);

        // XOR the counterOut with the plaintext producing the cipher text.
        for (int i = blockSize; --i >= 0; )
          out[outOff + i] = (byte) (counterOut[i] ^ in[inOff + i]);

        blockCounter++;

        return blockSize;
    }

    private void updateCounter() {
        long block = this.blockCounter;
        for (int i = blockSize; --i >= 0; ) {
            block += IV[i] & 0xff;
            counterIn[i] = (byte) block;
            block >>>= 8;
        }
    }

    public void setBlockCounter(long block) {
        blockCounter = block;
    }

    public long getBlockCounter() {
        return blockCounter;
    }

    public void reset() {
        // Effectively the same as setBlockCounter(0).
        //System.arraycopy(IV, 0, counterIn, 0, blockSize);
        blockCounter = 0;

        cipher.reset();
    }
}