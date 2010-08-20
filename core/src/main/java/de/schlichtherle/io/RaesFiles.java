/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.schlichtherle.io;

import de.schlichtherle.crypto.io.raes.KeyManagerRaesParameters;
import de.schlichtherle.crypto.io.raes.RaesOutputStream;
import de.schlichtherle.crypto.io.raes.RaesParameters;
import de.schlichtherle.crypto.io.raes.RaesReadOnlyFile;
import de.schlichtherle.io.rof.ReadOnlyFileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Saves and restores the contents of arbitrary files to and from the RAES
 * file format for encryption and decryption.
 * Note that this class is not intended to access RAES encrypted ZIP files -
 * use the {@link File} class for this task instead.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class RaesFiles {

    /** You cannot instantiate this class. */
    protected RaesFiles() {
    }

    /**
     * Encrypts the given plain file to the given RAES file.
     * This version uses the default ArchiveDetector to detect any archive
     * files in its parent directory path except the files themselves, which
     * are not recognized as archive files.
     */
    public static void encrypt(
            final String plainFilePath,
            final String raesFilePath)
    throws IOException {
        encrypt(plainFilePath, raesFilePath, File.getDefaultArchiveDetector());
    }

    /**
     * Encrypts the given plain file to the given RAES file,
     * using the provided ArchiveDetector to detect any archive files in its
     * parent directory path except the files themselves, which are not
     * recognized as archive files.
     */
    public static void encrypt(
            final String plainFilePath,
            final String raesFilePath,
            final ArchiveDetector detector)
    throws IOException {
        final File plainFile = getNonArchiveFile(plainFilePath, detector);
        final InputStream in = detector.createFileInputStream(plainFile);
        final RaesOutputStream out;
        try {
            final File raesFile = getNonArchiveFile(raesFilePath, detector);
            final RaesParameters params = new KeyManagerRaesParameters(
                    raesFile.getCanonicalPath()); // use canonical path name
            out = RaesOutputStream.getInstance(
                    detector.createFileOutputStream(raesFile, false),
                    params);
        } catch (IOException failure) {
            in.close();
            throw failure;
        }
        File.cp(in, out);
    }

    /**
     * Decrypts the given RAES file to the given plain file.
     * This version uses the default ArchiveDetector to detect any archive
     * files in its parent directory path except the files themselves, which
     * are not recognized as archive files.
     */
    public static void decrypt(
            final String raesFilePath,
            final String plainFilePath,
            final boolean strongAuthentication)
    throws IOException {
        decrypt(raesFilePath, plainFilePath, strongAuthentication,
                File.getDefaultArchiveDetector());
    }

    /**
     * Decrypts the given RAES file to the given plain file,
     * using the provided ArchiveDetector to detect any archvie files in its
     * parent directory path except the files themselves, which are not
     * recognized as archive files.
     * 
     * @param strongAuthentication If this is {@code true}, the whole
     *        contents of encrypted file get authenticated, which can be a
     *        time consuming operation.
     *        Otherwise, only the key/password and the file length get
     *        authenticated.
     */
    public static void decrypt(
            final String raesFilePath,
            final String plainFilePath,
            final boolean strongAuthentication,
            final ArchiveDetector detector)
    throws IOException {
        final File raesFile = getNonArchiveFile(raesFilePath, detector);
        final RaesParameters params = new KeyManagerRaesParameters(
                raesFile.getCanonicalPath()); // use canonical path name
        final RaesReadOnlyFile rrof
                = RaesReadOnlyFile.getInstance(raesFile, params);
        final InputStream in;
        final OutputStream out;
        try {
            if (strongAuthentication)
                rrof.authenticate();
            in = new ReadOnlyFileInputStream(rrof);
            final File plainFile = getNonArchiveFile(plainFilePath, detector);
            out = detector.createFileOutputStream(plainFile, false);
        } catch (IOException failure) {
            rrof.close();
            throw failure;
        }
        File.cp(in, out);
    }

    /**
     * Creates a file object which uses the provided ArchiveDetector,
     * but does not recognize its own pathname as an archive file.
     * Please note that this method just creates a file object,
     * and does not actually operate on the file system.
     */
    private static File getNonArchiveFile(
            final String path,
            final ArchiveDetector detector) {
        final File file = detector.createFile(path);
        return ArchiveDetector.NULL.createFile(file.getParentFile(), file.getName());
    }
}
