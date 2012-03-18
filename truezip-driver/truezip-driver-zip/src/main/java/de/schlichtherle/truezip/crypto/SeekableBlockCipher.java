/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
