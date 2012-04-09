/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.crypto;

import de.truezip.kernel.io.ReadOnlyChannelITSuite;
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
                Files.newByteChannel(path),
                new SeekableNullEngine());
    }

    private static final class SeekableNullEngine
    extends NullEngine
    implements SeekableBlockCipher {
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
