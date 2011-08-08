/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.crypto.SICSeekableBlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.engines.AESFastEngine;

/**
 * Implements Counter (CTR) mode (alias Segmented Integer Counter - SIC)
 * on top of an AES engine.
 * This class is almost identical to {@link SICSeekableBlockCipher} except that
 * the counter is reset to one and updated in little endian order.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class WinZipAesCipherMode extends SICSeekableBlockCipher {

    /**
     * Constructs a new block cipher mode for use with WinZip AES.
     */
    WinZipAesCipherMode() {
        super(new AESFastEngine());
    }

    @Override
    public int processBlock(
            final byte[] in,
            int inOff,
            final byte[] out,
            int outOff)
    throws DataLengthException, IllegalStateException {
        updateCounter();
        this.cipher.processBlock(this.cipherIn, 0, this.cipherOut, 0);

        // XOR the cipherOut with the plaintext producing the cipher text.
        final int blockSize = this.blockSize;
        {
            int i = blockSize;
            inOff += i;
            outOff += i;
            while (i > 0)
                out[--outOff] = (byte) (in[--inOff] ^ this.cipherOut[--i]);
        }

        return blockSize;
    }

    private void updateCounter() {
        final int blockSize = this.blockSize;
        long blockCounter = this.blockCounter++;
        for (int i = 0; i < blockSize; i++) { // little endian order!
            blockCounter += IV[i] & 0xff;
            this.cipherIn[i] = (byte) blockCounter;
            blockCounter >>>= 8;
        }
    }

    @Override
    public void reset() {
        super.reset();
        this.blockCounter = 1;
    }
}
