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

/**
 * Extends a {@code BlockCipher} in order to support random access to
 * the blocks in a plain or cipher text.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface SeekableBlockCipher extends BlockCipher {

    /**
     * Sets the counter so that the block with the given index, starting
     * at 0, can be processed next.
     *
     * @param blockCounter The index of the block, starting at 0, which will be
     *        processed next when
     *        {@link #processBlock(byte[], int, byte[], int)} is called.
     */
    void setBlockCounter(long blockCounter);

    /**
     * Returns the index of the block, starting at 0, which will be processed
     * next when {@link #processBlock(byte[], int, byte[], int)} is called.
     */
    long getBlockCounter();
}
