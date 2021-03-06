/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import net.java.truevfs.comp.zip.crypto.CtrBlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;

/**
 * Implements Counter (CTR) mode (alias Segmented Integer Counter - SIC)
 * on top of an AES engine.
 * This class is almost identical to {@link CtrBlockCipher} except that
 * the block counter is incremented <em>before</em> updating the cipher input
 * in <em>little endian</em> order.
 *
 * @see    <a href="http://www.gladman.me.uk/cryptography_technology/fileencrypt/">A Password Based File Encyption Utility (Dr. Gladman)</a>
 * @author Christian Schlichtherle
 */
final class WinZipAesCipher extends CtrBlockCipher {

    /**
     * Constructs a new block cipher mode for use with WinZip AES.
     * This constructor uses an {@link AESEngine} as the underlying block
     * cipher.
     */
    WinZipAesCipher() {
        super(new AESEngine());
    }

    @Override
    protected void incCounter() {
        final int blockSize = this.blockSize;
        long blockCounter = ++this.blockCounter; // pre-increment the block counter!
        for (int i = 0; i < blockSize; i++) { // little endian order!
            blockCounter += IV[i] & 0xff;
            cipherIn[i] = (byte) blockCounter;
            blockCounter >>>= 8;
        }
    }
}
