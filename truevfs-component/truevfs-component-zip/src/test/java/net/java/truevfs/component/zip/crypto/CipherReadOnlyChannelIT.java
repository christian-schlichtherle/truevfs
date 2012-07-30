/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.zip.crypto;

import net.java.truevfs.component.zip.crypto.SeekableBlockCipher;
import net.java.truevfs.component.zip.crypto.CipherReadOnlyChannel;
import net.java.truecommons.io.ReadOnlyChannelITSuite;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bouncycastle.crypto.engines.NullEngine;

/**
 * @author Christian Schlichtherle
 */
public class CipherReadOnlyChannelIT extends ReadOnlyChannelITSuite {

    @Override
    protected SeekableByteChannel newChannel(Path path) throws IOException {
        return new CipherReadOnlyChannel(
                new SeekableNullEngine(), Files.newByteChannel(path));
    }

    private static final class SeekableNullEngine
    extends NullEngine implements SeekableBlockCipher {
        long blockCounter;

        SeekableNullEngine() {
            init(true, null);
        }

        @Override
        public void setBlockCounter(final long blockCounter) {
            this.blockCounter = blockCounter;
        }

        @Override
        public long getBlockCounter() {
            return blockCounter;
        }
    }
}
