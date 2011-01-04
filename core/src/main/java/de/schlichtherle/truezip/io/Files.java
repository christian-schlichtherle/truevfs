/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static java.io.File.*;

/**
 * Provides static utility methods for file objects and path names.
 * This class cannot get instantiated outside its package.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Files extends Paths {

    Files() {
    }

    /**
     * Returns the directory for temporary files.
     * By default, this method returns {@code null}, which means that
     * the directory used for {@link #createTempFile} is determined by the
     * system property {@code java.io.tmpdir}.
     */
    public static File getTempDirectory() {
        return tempDirectory;
    }

    /**
     * Sets the directory for temporary files.
     * If this is {@code null}, the value of the system property
     * {@code java.io.tmpdir} is used by {@link #createTempFile}.
     */
    public static void setTempDirectory(final File directory) {
        Files.tempDirectory = directory;
    }

    private static File tempDirectory;

    /**
     * Equivalent to
     * {@link #createTempFile(String, String, File) createTempFile(prefix, null, null)}.
     */
    public static File createTempFile(String prefix)
    throws IOException {
        return createTempFile(prefix, null, null);
    }

    /**
     * Equivalent to
     * {@link #createTempFile(String, String, File) createTempFile(prefix, suffix, null)}.
     */
    public static File createTempFile(String prefix, String suffix)
    throws IOException {
        return createTempFile(prefix, suffix, null);
    }

    /**
     * Like {@link File#createTempFile}, but uses the value of the
     * class property {@code tempDirectory} as the directory for temporary
     * files.
     * If the value of this property is {@code null}, the directory is
     * determined by the value of the system property {@code java.io.tmpdir}.
     *
     * @see #getTempDirectory
     * @see #setTempDirectory
     */
    public static File createTempFile(  String prefix,
                                        String suffix,
                                        File directory)
    throws IOException {
        return File.createTempFile(prefix, suffix,
                null == directory ? tempDirectory : directory);
    }

    public static @NonNull String getRealPath(@NonNull File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException ex) {
            return normalize(file.getAbsolutePath());
        }
    }

    /**
     * Returns the canonical path of the given file or the normalized absolute
     * path if canonicalizing the path fails due to an {@code IOException}.
     *
     * @param  file the file.
     * @return The canonical or absolute path of this file as a
     *         {@code File} instance.
     */
    public static @NonNull File getRealFile(@NonNull File file) {
        String p = getRealPath(file);
        return p.equals(file.getPath()) ? file : new File(p);
    }

    private static File getRealFile0(final File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            final File parent = file.getParentFile();
            return null != parent
                    ? new File(getRealFile0(parent), file.getName())
                    : file.getAbsoluteFile();
        }
    }

    /**
     * Returns {@code true} if the given file can be created or exists
     * and at least one byte can be successfully written to it - the file is
     * restored to its previous state afterwards.
     * This is a much stronger test than {@link File#canWrite()}.
     */
    public static boolean isCreatableOrWritable(final File file) {
        try {
            if (file.createNewFile()) {
                return isCreatableOrWritable(file) && file.delete();
            } else if (file.canWrite()) {
                // Some operating and file system combinations make File.canWrite()
                // believe that the file is writable although it's not.
                // We are not that gullible, so let's test this...
                final long time = file.lastModified();
                if (time < 0) {
                    // lastModified() may return negative values but setLastModified()
                    // throws an IAE for negative values, so we are conservative.
                    // See issue #18.
                    return false;
                }
                if (!file.setLastModified(time + 1)) {
                    // This may happen on Windows and normally means that
                    // somebody else has opened this file
                    // (regardless of read or write mode).
                    // Be conservative: We don't allow writing to this file!
                    return false;
                }
                boolean ok;
                try {
                    // Open the file for reading and writing, requiring any
                    // update to its contents to be written to the filesystem
                    // synchronously.
                    // As Dr. Simon White from Catalysoft, Cambridge, UK reported,
                    // "rws" does NOT work on Mac OS X with Apple's Java 1.5
                    // Release 1 (equivalent to Sun's Java 1.5.0_02), however
                    // it DOES work with Apple's Java 1.5 Release 3.
                    // He also confirmed that "rwd" works on Apple's
                    // Java 1.5 Release 1, so we use this instead.
                    // Thank you very much for spending the time to fix this
                    // issue, Dr. White!
                    final RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                    try {
                        final boolean empty;
                        int octet = raf.read();
                        if (octet == -1) {
                            octet = 0; // assume first byte is 0
                            empty = true;
                        } else {
                            empty = false;
                        }
                        // Let's test if we can overwrite the first byte.
                        // See issue #29.
                        raf.seek(0);
                        raf.write(octet);
                        try {
                            // Rewrite original content and check success.
                            raf.seek(0);
                            final int check = raf.read();
                            // This should always return true unless the storage
                            // device is faulty.
                            ok = octet == check;
                        } finally {
                            if (empty)
                                raf.setLength(0);
                        }
                    } finally {
                        raf.close();
                    }
                } finally {
                    if (!file.setLastModified(time)) {
                        // This may happen on Windows and normally means that
                        // somebody else has opened this file meanwhile
                        // (regardless of read or write mode).
                        // Be conservative: We don't allow (further) writing to
                        // this file!
                        ok = false;
                    }
                }
                return ok;
            } else { // if (!file.canWrite()) {
                return false;
            }
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Equivalent to {@link #normalize(String, char)
     * normalize(path, File.separatorChar)}.
     */
    public static String normalize(String path) {
        return normalize(path, separatorChar);
    }

    /**
     * Removes all redundant separators, dot directories ({@code "."}) and
     * dot-dot directories ({@code ".."}) from the path name and returns the
     * result.
     * Trailing separator characters are removed and a single {@code "."} gets
     * truncated to an empty path.
     *
     * @param file The file instance which's path is to be normalized.
     * @return {@code file} if it was already in normalized form.
     *         Otherwise, an object which's runtime class is guaranteed to
     *         be {@code File}.
     */
    public static File normalize(File file) {
        String p = file.getPath();
        String n = normalize(p);
        return n.equals(p) ? file : new File(n);
    }

    /**
     * Equivalent to {@link #split(String, char)
     * split(path, File.separatorChar)}.
     */
    public static Splitter split(String path) {
        return split(path, separatorChar);
    }

    /**
     * Equivalent to {@link #isAbsolute(String, char)
     * isAbsolute(path, File.separatorChar)}.
     */
    public static boolean isAbsolute(String path) {
        return isAbsolute(path, separatorChar);
    }

    /**
     * Returns true if and only if the path name represented by {@code a}
     * contains the path name represented by {@code b}.
     *
     * @param a A non-{@code null} {@link String} reference.
     * @param b A non-{@code null} {@link String} reference.
     * @throws NullPointerException If any parameter is {@code null}.
     */
    public static boolean contains(String a, String b) {
        return contains(a, b, separatorChar);
    }

    /**
     * Returns {@code true} if and only if the path represented
     * by {@code a} contains the path represented by {@code b},
     * where a path is said to contain another path if and only
     * if it is equal or a parent of the other path.
     * <p>
     * <b>Note:</b>
     * <ul>
     * <li>This method uses the canonical path name of the given files or,
     *     if failing to canonicalize the path names, the normalized absolute
     *     path names in order to compute reliable results.
     * <li>This method does <em>not</em> access the file system.
     *     It just tests the path names.
     * </ul>
     *
     * @param a A non-{@code null} {@link File} reference.
     * @param b A non-{@code null} {@link File} reference.
     * @throws NullPointerException If any parameter is {@code null}.
     */
    public static boolean contains(File a, File b) {
        return contains(getRealFile(a).getPath(), getRealFile(b).getPath());
    }

    /** The prefix of a UNC (a Windows concept). */
    private static final String uncPrefix = separator + separator;

    /**
     * Returns {@code true} if and only if the given path is a UNC.
     * Note that this may be only relevant on the Windows platform.
     */
    public static boolean isUNC(String path) {
        return path.startsWith(uncPrefix) && path.indexOf(separatorChar, 2) > 2;
    }

    public static boolean isUNC(File file) {
        return isUNC(getRealFile(file).getPath());
    }
}
