/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.crypto;

import de.truezip.kernel.io.AbstractSink;
import de.truezip.kernel.io.ReadOnlyChannelITSuite;
import de.truezip.kernel.io.Streams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Christian Schlichtherle
 */
public final class RaesReadOnlyChannelIT extends ReadOnlyChannelITSuite {

    private static final Logger
            logger = Logger.getLogger(RaesReadOnlyChannelIT.class.getName());

    private static RaesParameters newRaesParameters() {
        return new MockType0RaesParameters();
    }

    private Path cipherFile;

    @Override
    protected SeekableByteChannel newChannel(final Path plainFile)
    throws IOException {
        try (final InputStream in = newInputStream(plainFile)) {
            cipherFile = createTempFile(TEMP_FILE_PREFIX, null);
            try {
                final RaesOutputStream out = new RaesSink(
                        new AbstractSink() {
                            @Override
                            public OutputStream newStream() throws IOException {
                                return newOutputStream(cipherFile);
                            }
                        },
                        newRaesParameters()).newStream();
                Streams.copy(in, out);
                logger.log(Level.FINEST,
                        "Encrypted {0} bytes of random data using AES-{1}/CTR/Hmac-SHA-256/PKCS#12v1",
                        new Object[]{ Files.size(plainFile), out.getKeyStrength().getBits() });
                // Open cipherFile for random access decryption.
            } catch (final Throwable ex) {
                final Path cipherFile = this.cipherFile;
                this.cipherFile = null;
                deleteIfExists(cipherFile);
                throw ex;
            }
            return RaesReadOnlyChannel.getInstance(cipherFile, newRaesParameters());
        }
    }

    @Override
    public void tearDown() {
        try {
            try {
                super.tearDown();
            } finally {
                final Path cipherFile = this.cipherFile;
                this.cipherFile = null;
                if (null != cipherFile)
                    deleteIfExists(cipherFile);
            }
        } catch (final IOException ex) {
            logger.log(Level.FINEST,
                    "Failed to clean up test file (this may be just an aftermath):",
                    ex);
        }
    }
}
