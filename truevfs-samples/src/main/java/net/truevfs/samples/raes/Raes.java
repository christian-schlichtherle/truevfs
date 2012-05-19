/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.samples.raes;

import net.truevfs.driver.zip.raes.KeyManagerRaesParameters;
import net.truevfs.driver.zip.raes.crypto.RaesOutputStream;
import net.truevfs.driver.zip.raes.crypto.RaesReadOnlyChannel;
import net.truevfs.kernel.io.AbstractSink;
import net.truevfs.kernel.io.AbstractSource;
import net.truevfs.kernel.io.Streams;
import net.truevfs.key.sl.KeyManagerLocator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saves and restores the contents of arbitrary files to and from the RAES
 * file format for encryption and decryption.
 * This class cannot get instantiated outside its package.
 * <p>
 * Note that this class is not intended to access RAES encrypted ZIP files -
 * use the TrueVFS client API modules for this task instead.
 *
 * @author Christian Schlichtherle
 */
public final class Raes {

    /** Can't touch this - hammer time! */
    private Raes() { }

    /**
     * Encrypts the given plain file to the given RAES file.
     */
    public static void encrypt(final Path plain, final Path cipher)
    throws IOException {
        Streams.copy(
                new AbstractSource() {
                    @Override
                    public InputStream stream() throws IOException {
                        return Files.newInputStream(plain);
                    }
                },
                new AbstractSink() {
                    @Override
                    public OutputStream stream() throws IOException {
                        return RaesOutputStream.create(
                                new KeyManagerRaesParameters(
                                    KeyManagerLocator.SINGLETON,
                                    cipher/*.getCanonicalFile()*/.toUri()),
                                new AbstractSink() {
                                    @Override
                                    public OutputStream stream() throws IOException {
                                        return Files.newOutputStream(cipher);
                                    }
                                });
                    }
                });
    }

    /**
     * Decrypts the given RAES file to the given plain file.
     * 
     * @param authenticate If this is {@code true}, the channel data get
     *        authenticated.
     *        Note that this operation has linear complexity.
     *        If this is {@code false}, only the key/password and the file
     *        length get authenticated.
     */
    public static void decrypt(
            final Path cipherFile,
            final Path plainFile,
            final boolean authenticate)
    throws IOException {
        Streams.copy(
                new AbstractSource() {
                    @Override
                    public SeekableByteChannel channel() throws IOException {
                        final RaesReadOnlyChannel
                                channel = RaesReadOnlyChannel.create(
                                    new KeyManagerRaesParameters(
                                        KeyManagerLocator.SINGLETON,
                                        cipherFile/*.getCanonicalFile()*/.toUri()),
                                    new AbstractSource() {
                                        @Override
                                        public SeekableByteChannel channel() throws IOException {
                                            return Files.newByteChannel(cipherFile);
                                        }
                                    });
                        if (authenticate)
                            channel.authenticate();
                        return channel;
                    }
                },
                new AbstractSink() {
                    @Override
                    public OutputStream stream() throws IOException {
                        return Files.newOutputStream(plainFile);
                    }
                });
    }
}
