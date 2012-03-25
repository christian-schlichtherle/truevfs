/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.sample.file;

import de.truezip.driver.zip.raes.crypto.RaesOutputStream;
import de.truezip.driver.zip.raes.crypto.RaesParameters;
import de.truezip.driver.zip.raes.crypto.RaesReadOnlyFile;
import de.truezip.driver.zip.raes.crypto.param.KeyManagerRaesParameters;
import de.truezip.file.*;
import de.truezip.kernel.sl.KeyManagerLocator;
import de.schlichtherle.truezip.rof.DefaultReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Saves and restores the contents of arbitrary files to and from the RAES
 * file format for encryption and decryption.
 * This class cannot get instantiated outside its package.
 * <p>
 * Note that this class is not intended to access RAES encrypted ZIP files -
 * use the {@link TFile} class for this task instead.
 *
 * @author  Christian Schlichtherle
 */
public class RaesFiles {

    /** You cannot instantiate this class. */
    private RaesFiles() {
    }

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
        final TFile plainFile = newNonArchiveFile(plainFilePath, detector);
        final TFile raesFile = newNonArchiveFile(raesFilePath, detector);
        final RaesParameters params = new KeyManagerRaesParameters(
                KeyManagerLocator.SINGLETON,
                raesFile/*.getCanonicalFile()*/.toURI());
        final InputStream in = new TFileInputStream(plainFile);
        RaesOutputStream out = null;
        try {
            out = RaesOutputStream.getInstance(
                    new TFileOutputStream(raesFile, false),
                    params);
        } finally {
            if (null == out) // exception?
                in.close();
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
        final TFile raesFile = newNonArchiveFile(raesFilePath, detector);
        final TFile plainFile = newNonArchiveFile(plainFilePath, detector);
        final RaesParameters params = new KeyManagerRaesParameters(
                KeyManagerLocator.SINGLETON,
                raesFile/*.getCanonicalFile()*/.toURI());
        final ReadOnlyFile rof = new DefaultReadOnlyFile(raesFile);
        try {
            final RaesReadOnlyFile rrof
                    = RaesReadOnlyFile.getInstance(rof, params);
            if (authenticate)
                rrof.authenticate();
            final InputStream in = new ReadOnlyFileInputStream(rrof);
            OutputStream out = null;
            try {
                out = new TFileOutputStream(plainFile, false);
            } finally {
                if (null == out) // exception?
                    in.close();
            }
            TFile.cp(in, out);
        } finally {
            rof.close();
        }
    }

    /**
     * Creates a file object which uses the provided TArchiveDetector,
     * but does not recognize its own pathname as an archive file.
     * Please note that this method just creates a file object,
     * and does not actually operate on the file system.
     */
    private static TFile newNonArchiveFile(
            String path,
            TArchiveDetector detector) {
        TFile file = new TFile(path, detector);
        return new TFile(file.getParentFile(), file.getName(), TArchiveDetector.NULL);
    }
}