/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto;

import de.schlichtherle.truezip.rof.DefaultReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFileITSuite;
import java.io.File;
import java.io.IOException;
import org.bouncycastle.crypto.engines.NullEngine;

/**
 * @author Christian Schlichtherle
 */
public class CipherReadOnlyFileIT extends ReadOnlyFileITSuite {

    @Override
    protected ReadOnlyFile newReadOnlyFile(final File file) throws IOException {
        return new CipherReadOnlyFile(new DefaultReadOnlyFile(file)) {
            { init(new SeekableNullEngine(), 0, file.length()); }
        };
    }

    private static final class SeekableNullEngine
    extends NullEngine
    implements SeekableBlockCipher {
        long blockCounter;

        SeekableNullEngine() {
            init(true, null);
        }

        @Override
        public void setBlockCounter(long blockCounter) {
            this.blockCounter = blockCounter;
        }

        @Override
        public long getBlockCounter() {
            return blockCounter;
        }
    }
}
