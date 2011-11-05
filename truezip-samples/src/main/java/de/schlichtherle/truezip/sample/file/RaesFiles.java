/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.sample.file;

import de.schlichtherle.truezip.crypto.raes.param.KeyManagerRaesParameters;
import de.schlichtherle.truezip.crypto.raes.RaesOutputStream;
import de.schlichtherle.truezip.crypto.raes.RaesParameters;
import de.schlichtherle.truezip.crypto.raes.RaesReadOnlyFile;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;
import de.schlichtherle.truezip.key.sl.KeyManagerLocator;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFileInputStream;
import de.schlichtherle.truezip.rof.DefaultReadOnlyFile;
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
 * @version $Id$
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
        encrypt(plainFilePath, raesFilePath, TFile.getDefaultArchiveDetector());
    }

    /**
     * Encrypts the given plain file to the given RAES file,
     * using the provided TArchiveDetector to detect any archive files in its
     * parent directory path except the files themselves, which are not
     * recognized as archive files.
     */
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
        try {
            final RaesOutputStream out = RaesOutputStream.getInstance(
                    new TFileOutputStream(raesFile, false),
                    params);
            TFile.cp(in, out);
        } finally {
            in.close();
        }
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
                TFile.getDefaultArchiveDetector());
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
            final OutputStream out = new TFileOutputStream(plainFile, false);
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
