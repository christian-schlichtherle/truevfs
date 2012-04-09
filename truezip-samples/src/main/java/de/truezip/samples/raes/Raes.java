/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.samples.raes;

import de.truezip.driver.zip.raes.KeyManagerRaesParameters;
import de.truezip.driver.zip.raes.crypto.RaesOutputStream;
import de.truezip.driver.zip.raes.crypto.RaesReadOnlyChannel;
import de.truezip.file.TArchiveDetector;
import de.truezip.file.TConfig;
import de.truezip.kernel.io.AbstractSink;
import de.truezip.kernel.io.AbstractSource;
import de.truezip.kernel.io.ChannelInputStream;
import de.truezip.kernel.io.Streams;
import de.truezip.key.sl.KeyManagerLocator;
import de.truezip.path.TPath;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import static java.nio.file.Files.*;
import javax.annotation.WillClose;

/**
 * Saves and restores the contents of arbitrary files to and from the RAES
 * file format for encryption and decryption.
 * This class cannot get instantiated outside its package.
 * <p>
 * Note that this class is not intended to access RAES encrypted ZIP files -
 * use the TrueZIP client API modules for this task instead.
 *
 * @author Christian Schlichtherle
 */
public final class Raes {

    /** Can't touch this - hammer time! */
    private Raes() { }

    /**
     * Encrypts the given plain file to the given RAES file.
     * This version uses the default TArchiveDetector to detect any archive
     * files in its parent directory path except the files themselves, which
     * are not recognized as archive files.
     */
    public static void encrypt(
            final String plainPath,
            final String cipherPath)
    throws IOException {
        encrypt(plainPath, cipherPath, TConfig.get().getArchiveDetector());
    }

    /**
     * Encrypts the given plain file to the given RAES file,
     * using the provided TArchiveDetector to detect any archive files in its
     * parent directory path except the files themselves, which are not
     * recognized as archive files.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public static void encrypt(
            final String plainPath,
            final String cipherPath,
            final TArchiveDetector detector)
    throws IOException {
        try (final TConfig config = TConfig.push()) {
            config.setArchiveDetector(detector);
            final TPath plainFile = new TPath(plainPath).toNonArchivePath();
            final TPath cipherFile = new TPath(cipherPath).toNonArchivePath();
            final @WillClose InputStream in = newInputStream(plainFile);
            final @WillClose OutputStream out;
            try {
                out = RaesOutputStream.create(
                        new KeyManagerRaesParameters(
                            KeyManagerLocator.SINGLETON,
                            cipherFile/*.getCanonicalFile()*/.toUri()),
                        new AbstractSink() {
                            @Override
                            public OutputStream newStream() throws IOException {
                                return newOutputStream(cipherFile);
                            }
                        });
            } catch (final Throwable ex) {
                try {
                    in.close();
                } catch (final Throwable ex2) {
                    ex.addSuppressed(ex2);
                }
                throw ex;
            }
            Streams.copy(in, out);
        }
    }

    /**
     * Decrypts the given RAES file to the given plain file.
     * This version uses the default TArchiveDetector to detect any archive
     * files in its parent directory path except the files themselves, which
     * are not recognized as archive files.
     */
    public static void decrypt(
            final String cipherPath,
            final String plainPath,
            final boolean strongAuthentication)
    throws IOException {
        decrypt(cipherPath, plainPath, strongAuthentication,
                TConfig.get().getArchiveDetector());
    }

    /**
     * Decrypts the given RAES file to the given plain file,
     * using the provided TArchiveDetector to detect any archvie files in its
     * parent directory path except the files themselves, which are not
     * recognized as archive files.
     * 
     * @param authenticate If this is {@code true}, the entire contents of the
     *        encrypted file get authenticated, which can be a time consuming
     *        operation.
     *        Otherwise, only the key/password and the file length get
     *        authenticated.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public static void decrypt(
            final String cipherPath,
            final String plainPath,
            final boolean authenticate,
            final TArchiveDetector detector)
    throws IOException {
        try (final TConfig config = TConfig.push()) {
            config.setArchiveDetector(detector);
            final TPath cipherFile = new TPath(cipherPath).toNonArchivePath();
            final TPath plainFile = new TPath(plainPath).toNonArchivePath();
            final RaesReadOnlyChannel sbc = RaesReadOnlyChannel.create(
                    new KeyManagerRaesParameters(
                    KeyManagerLocator.SINGLETON,
                    cipherFile/*.getCanonicalFile()*/.toUri()),
                    new AbstractSource() {
                        @Override
                        public SeekableByteChannel newChannel() throws IOException {
                            return newByteChannel(cipherFile);
                        }
                    });
            final @WillClose InputStream in = new ChannelInputStream(sbc);
            @WillClose OutputStream out = null;
            try {
                if (authenticate)
                    sbc.authenticate();
                out = newOutputStream(plainFile);
            } catch (final Throwable ex) {
                try {
                    in.close();
                } catch (final Throwable ex2) {
                    ex.addSuppressed(ex2);
                }
                throw ex;
            }
            Streams.copy(in, out);
        }
    }
}
