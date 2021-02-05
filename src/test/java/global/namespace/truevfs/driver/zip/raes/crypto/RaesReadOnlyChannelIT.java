/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.raes.crypto;

import global.namespace.truevfs.commons.io.AbstractSink;
import global.namespace.truevfs.commons.io.AbstractSource;
import global.namespace.truevfs.commons.io.Streams;
import global.namespace.truevfs.it.io.ReadOnlyChannelITSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Files.*;

/**
 * @author Christian Schlichtherle
 */
public final class RaesReadOnlyChannelIT extends ReadOnlyChannelITSuite {

    private static final Logger logger = LoggerFactory.getLogger(RaesReadOnlyChannelIT.class);

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
                        "Encrypted {} bytes of random data using AES-{}/CTR/Hmac-SHA-256/PKCS#12v1",
                        Files.size(plainFile), out.getKeyStrength().getBits());
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
