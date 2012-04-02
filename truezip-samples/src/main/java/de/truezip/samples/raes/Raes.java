/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.samples.raes;

import de.truezip.driver.zip.raes.KeyManagerRaesParameters;
import de.truezip.driver.zip.raes.crypto.RaesParameters;
import de.truezip.driver.zip.raes.crypto.RaesReadOnlyFile;
import de.truezip.driver.zip.raes.crypto.RaesSink;
import de.truezip.file.*;
import de.truezip.kernel.io.AbstractSink;
import de.truezip.kernel.io.Sink;
import de.truezip.kernel.rof.DefaultReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFileInputStream;
import de.truezip.key.sl.KeyManagerLocator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.WillClose;

/**
 * Saves and restores the contents of arbitrary files to and from the RAES
 * file format for encryption and decryption.
 * This class cannot get instantiated outside its package.
 * <p>
 * Note that this class is not intended to access RAES encrypted ZIP files -
 * use the {@link TFile} class for this task instead.
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
            final String plainFilePath,
            final String raesFilePath)
    throws IOException {
        encrypt(plainFilePath, raesFilePath, TConfig.get().getArchiveDetector());
    }

    /**
     * Encrypts the given plain file to the given RAES file,
     * using the provided TArchiveDetector to detect any archive files in its
     * parent directory path except the files themselves, which are not
     * recognized as archive files.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public static void encrypt(
            final String plainFilePath,
            final String raesFilePath,
            final TArchiveDetector detector)
    throws IOException {
        final TFile plainFile = new TFile(plainFilePath, detector).toNonArchiveFile();
        final TFile raesFile = new TFile(raesFilePath, detector).toNonArchiveFile();
        final Sink sink = new RaesSink(
                new AbstractSink() {
                    @Override
                    public OutputStream newStream() throws IOException {
                        return new TFileOutputStream(raesFile);
                    }
                },
                new KeyManagerRaesParameters(
                    KeyManagerLocator.SINGLETON,
                    raesFile/*.getCanonicalFile()*/.toURI()));
        final @WillClose InputStream in = new TFileInputStream(plainFile);
        final @WillClose OutputStream out;
        try {
            out = sink.newStream();
        } catch (final IOException ex) {
            try {
                in.close();
            } catch (final IOException ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
        TFile.cp(in, out);
    }

    /**
     * Decrypts the given RAES file to the given plain file.
     * This version uses the default TArchiveDetector to detect any archive
     * files in its parent directory path except the files themselves, which
     * are not recognized as archive files.
     */
    public static void decrypt(
            final String raesFilePath,
            final String plainFilePath,
            final boolean strongAuthentication)
    throws IOException {
        decrypt(raesFilePath, plainFilePath, strongAuthentication,
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
            final String raesFilePath,
            final String plainFilePath,
            final boolean authenticate,
            final TArchiveDetector detector)
    throws IOException {
        final TFile raesFile = new TFile(raesFilePath, detector).toNonArchiveFile();
        final TFile plainFile = new TFile(plainFilePath, detector).toNonArchiveFile();
        final RaesParameters param = new KeyManagerRaesParameters(
                KeyManagerLocator.SINGLETON,
                raesFile/*.getCanonicalFile()*/.toURI());
        try (final ReadOnlyFile rof = new DefaultReadOnlyFile(raesFile)) {
            final RaesReadOnlyFile rrof
                    = RaesReadOnlyFile.getInstance(rof, param);
            if (authenticate)
                rrof.authenticate();
            final @WillClose InputStream in = new ReadOnlyFileInputStream(rrof);
            @WillClose OutputStream out = null;
            try {
                out = new TFileOutputStream(plainFile);
            } catch (final IOException ex) {
                try {
                    in.close();
                } catch (final IOException ex2) {
                    ex.addSuppressed(ex2);
                }
                throw ex;
            }
            TFile.cp(in, out);
        }
    }
}