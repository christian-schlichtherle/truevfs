/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.zip.crypto;

import org.bouncycastle.crypto.BlockCipher;

/**
 * Extends a {@code BlockCipher} in order to support random access to
 * the blocks in a plain or cipher text.
 *
 * @author Christian Schlichtherle
 */
public interface SeekableBlockCipher extends BlockCipher {

    /**
     * Returns the index of the block, starting at 0, which will be processed
     * next when {@link #processBlock(byte[], int, byte[], int)} is called.
     */
    long getBlockCounter();

    /**
     * Sets the counter so that the block with the given index, starting
     * at 0, can be processed next.
     *
     * @param blockCounter The index of the block, starting at 0, which will be
     *        processed next when
     *        {@link #processBlock(byte[], int, byte[], int)} is called.
     */
    void setBlockCounter(long blockCounter);
}
