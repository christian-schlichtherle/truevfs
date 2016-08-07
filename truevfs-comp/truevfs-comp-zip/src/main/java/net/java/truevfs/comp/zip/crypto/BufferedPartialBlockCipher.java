/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip.crypto;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;

/**
 * A buffered block cipher which allows a partial block when calling
 * {@link #doFinal(byte[], int)}.
 *
 * @author Christian Schlichtherle
 */
public final class BufferedPartialBlockCipher extends BufferedBlockCipher {

    public BufferedPartialBlockCipher(BlockCipher cipher) {
        super(cipher);
        partialBlockOkay = true;
    }
}
