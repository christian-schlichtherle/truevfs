/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes.crypto;

import net.java.truevfs.driver.zip.raes.crypto.RaesParameters;
import net.java.truevfs.driver.zip.raes.crypto.RaesReadOnlyChannel;
import net.java.truevfs.driver.zip.raes.crypto.RaesOutputStream;
import net.java.truecommons.io.AbstractSink;
import net.java.truecommons.io.AbstractSource;
import net.java.truecommons.io.ReadOnlyChannelITSuite;
import net.java.truecommons.io.Streams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Schlichtherle
 */
public final class RaesReadOnlyChannelIT extends ReadOnlyChannelITSuite {

    private static final Logger
            logger = LoggerFactory.getLogger(RaesReadOnlyChannelIT.class);

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
                final RaesOutputStream out = RaesOutputStream.create(
                        newRaesParameters(),
                        new AbstractSink() {
                            @Override
                            public OutputStream stream() throws IOException {
                                return newOutputStream(cipherFile);
                            }
                        });
                Streams.copy(in, out);
                logger.trace(
                        "Encrypted {} bytes of random data using AES-{1}/CTR/Hmac-SHA-256/PKCS#12v1",
                        new Object[]{ Files.size(plainFile), out.getKeyStrength().getBits() });
                // Open cipherFile for random access decryption.
            } catch (final Throwable ex) {
                final Path cipherFile = this.cipherFile;
                this.cipherFile = null;
                deleteIfExists(cipherFile);
                throw ex;
            }
            return RaesReadOnlyChannel.create(
                    newRaesParameters(),
                    new AbstractSource() {
                        @Override
                        public SeekableByteChannel channel() throws IOException {
                            return newByteChannel(cipherFile);
                        }
                    });
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
            logger.trace(
                    "Failed to clean up test file (this may be just an aftermath):",
                    ex);
        }
    }
}
