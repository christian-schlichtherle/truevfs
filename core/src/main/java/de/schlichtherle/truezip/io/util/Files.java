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
package de.schlichtherle.truezip.io.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A utility class for creating temporary files.
 * This class allows to change the directory for temporary files via the class
 * property {@code directory}.
 * If the value of this property is {@code null} (which is the default),
 * the value of the system property {@code java.io.tmpdir} is used.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Files {

    /** You cannot instantiate this class. */
    protected Files() {
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
     * Like {@link File#createTempFile}, but uses the value of the
     * class property {@code tempDirectory} as the directory for temporary
     * files.
     * If the value of this property is {@code null}, the directory is
     * determined by the value of the system property {@code java.io.tmpdir}.
     *
     * @see #getTempDirectory
     * @see #setTempDirectory
     */
    public static File createTempFile(final String prefix, final String suffix)
    throws IOException {
        return File.createTempFile(prefix, suffix, tempDirectory);
    }

    /**
     * Like {@link #createTempFile(String, String)}, but uses the default
     * suffix {@code ".tmp"}.
     *
     * @see #getTempDirectory
     * @see #setTempDirectory
     */
    public static File createTempFile(final String prefix)
    throws IOException {
        return File.createTempFile(prefix, null, tempDirectory);
    }

    /**
     * Returns the canonical form of the given file or the normalized absolute
     * form if resolving the prior fails.
     *
     * @return The canonical or absolute path of this file as a
     *         {@code File} instance.
     * @throws NullPointerException If {@code file} is {@code null}.
     */
    public static File getCanOrAbsFile(final File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            final File parent = file.getParentFile();
            return normalize(parent != null
                    ? new File(getCanOrAbsFile0(parent), file.getName())
                    : file.getAbsoluteFile());
        }
    }

    private static File getCanOrAbsFile0(final File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            final File parent = file.getParentFile();
            return parent != null
                    ? new File(getCanOrAbsFile0(parent), file.getName())
                    : file.getAbsoluteFile();
        }
    }

    /**
     * Returns {@code true} if the given file exists or can be created
     * and at least one byte can be successfully written to it - the file is
     * restored to its previous state afterwards.
     * This is a much stronger test than {@link File#canWrite()}.
     */
    public static boolean isWritableOrCreatable(final File file) {
        try {
            if (!file.exists()) {
                final boolean created = file.createNewFile();
                boolean ok = isWritableOrCreatable(file);
                if (created && !file.delete()) {
                    ok = false; // be conservative!
                }
                return ok;
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
                            if (empty) {
                                raf.setLength(0);
                            }
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
            } else { // if (file.exists() && !file.canWrite()) {
                return false;
            }
        } catch (IOException ex) {
            return false; // don't allow writing if anything goes wrong!
        }
    }

    /**
     * Removes any {@code "."} and {@code ".."} directories from the path name
     * wherever possible.
     *
     * @param file The file instance which's path is to be normalized.
     * @return {@code file} if it was already in normalized form.
     *         Otherwise, an object which's runtime class is guaranteed to
     *         be {@code File}.
     */
    public static File normalize(final File file) {
        final String path = file.getPath();
        final String newPath = normalize(path, File.separatorChar);
        return newPath != path // mind contract of Paths.normalize!
                ? new File(newPath)
                : file;
    }

    /**
     * Equivalent to {@link #normalize(String, char)
     * normalize(path, File.separatorChar)}.
     */
    public static String normalize(final String path) {
        return normalize(path, File.separatorChar);
    }

    /**
     * Removes all redundant separators, dot directories ({@code "."}) and
     * dot-dot directories ({@code ".."}) from the path name and returns the
     * result.
     * An empty path results in {@code "."}.
     * On Windows, a path may be prefixed by a drive letter followed by a
     * colon.
     * On all platforms, a path may be prefixed by two leading separators
     * to indicate a UNC, although this is currently supported on Windows
     * only.
     * <p>
     * A single trailing separator character is always retained if present.
     *
     * @param path The name of the path to normalize.
     * @param separatorChar The path separator character.
     * @return {@code path} if it was already in normalized form.
     *         Otherwise, a new String with the normalized form of the given
     *         path.
     * @throws NullPointerException If path is {@code null}.
     */
    public static String normalize(
            final String path,
            final char separatorChar) {
        final int prefixLen = prefixLength(path, separatorChar);
        final int pathLen = path.length();
        final StringBuilder buffer = new StringBuilder(pathLen);
        normalize(path.substring(prefixLen, pathLen), separatorChar, 0, pathLen - prefixLen, buffer);
        buffer.insert(0, path.substring(0, prefixLen));
        if (buffer.length() == prefixLen
                && (prefixLen <= 0 || buffer.charAt(prefixLen - 1) != separatorChar))
            buffer.append('.');
        if (pathLen > 0 && path.charAt(pathLen - 1) == separatorChar)
            if (buffer.charAt(buffer.length() - 1) != separatorChar)
                buffer.append(separatorChar); // retain trailing separator
        final String result = buffer.length() != path.length()
                ? buffer.toString()
                : path;
        assert !result.equals(path) || result == path; // postcondition
        return result;
    }

    /**
     * Removes all redundant separators, dot directories ({@code "."}) and
     * dot-dot directories ({@code ".."}) from the path name and collects the
     * result in the given {@link StringBuilder}.
     * This is a recursive call: The top level call should provide
     * {@code 0} as the {@code toSkip} parameter, the length
     * of the path as the {@code end} parameter and an empty string
     * buffer as the {@code result} parameter.
     *
     * @param path The name of the path to normalize.
     *        {@code null} is not permissible.
     * @param separatorChar The path separator character.
     * @param skip The number of elements in the path to skip because they
     *        are followed by a dot-dot directory.
     *        This must not be negative.
     * @param end Only the string to the left of this index in
     *        {@code path} is considered.
     *        If not positive, nothing happens.
     * @param result The string builder with the collected results.
     *        {@code null} is not permissible.
     * @return The number of elements in the path which have not been skipped
     *         because there was an insufficient number of elements in the
     *         path.
     */
    private static int normalize(
            final String path,
            final char separatorChar,
            final int skip,
            final int end,
            final StringBuilder result) {
        assert skip >= 0;
        if (end <= 0)
            return 0;

        final int next = path.lastIndexOf(separatorChar, end - 1);
        final String base = path.substring(next + 1, end);
        final int skipped;
        if (base.length() == 0 || ".".equals(base)) {
            return normalize(path, separatorChar, skip, next, result);
        } else if ("..".equals(base)) {
            final int toSkip = skip + 1;
            skipped = normalize(path, separatorChar, toSkip, next, result);
            assert skipped <= toSkip;
            if (skipped == toSkip)
                return skip;
        } else if (skip > 0) {
            return normalize(path, separatorChar, skip - 1, next, result) + 1;
        } else {
            assert skip == 0;
            skipped = normalize(path, separatorChar, skip, next, result);
            assert skipped == 0;
        }

        final int resultLen = result.length();
        if (resultLen > 0 && result.charAt(resultLen - 1) != separatorChar)
            result.append(separatorChar);
        result.append(base);
        return skipped;
    }

    /**
     * Cuts off any separator characters at the end of the path, unless the
     * path contains of only separator characters, in which case a single
     * separator character is retained to denote the root directory.
     *
     * @return {@code path} if it's a path without trailing separators
     *         or contains the separator only.
     *         Otherwise, the substring until the first of at least one
     *         separating characters is returned.
     * @throws NullPointerException If path is {@code null}.
     */
    @SuppressWarnings("empty-statement")
    public static String cutTrailingSeparators(
            final String path,
            final char separatorChar) {
        int i = path.length();
        if (i <= 0 || path.charAt(--i) != separatorChar)
            return path;
        while (i > 0 && path.charAt(--i) == separatorChar)
            ;
        return path.substring(0, ++i);
    }

    /**
     * Equivalent to {@link #split(String, char)
     * split(path, File.separatorChar)}.
     */
    public static String[] split(
            final String path) {
        return split(path, File.separatorChar);
    }

    /**
     * Equivalent to {@link #split(String, char, String[])
     * split(path, separatorChar, new String[2])}.
     */
    public static String[] split(
            final String path,
            final char separatorChar) {
        return split(path, separatorChar, new String[2]);
    }

    /**
     * Splits a path into its parent path and its base name,
     * recognizing platform specific file system roots.
     *
     * @param path The name of the path which's parent path and base name
     *        are to be returned.
     * @param separatorChar The path separator character to use for this
     *        operation.
     * @param result An array of at least two {@link String} elements to hold
     *        the result upon return.
     * @return An array holding at least two strings:
     *         <ol>
     *         <li>Index zero holds the parent path or {@code null} if the
     *             path does not specify a parent. This name compares equal
     *             with {@link java.io.File#getParent()}.</li>
     *         <li>Index one holds the base name. This name compares
     *             equal with {@link java.io.File#getName()}.</li>
     *         </ol>
     * @return {@code result}
     * @throws NullPointerException If path is {@code null}.
     */
    public static String[] split(
            final String path,
            final char separatorChar,
            final String[] result) {
        final int prefixLen = prefixLength(path, separatorChar);

        // Skip any trailing separators and look for the previous separator.
        int baseBegin = -1;
        int baseEnd = path.length() - 1;
        if (prefixLen <= baseEnd) {
            baseEnd = lastIndexNot(path, separatorChar, baseEnd);
            baseBegin = path.lastIndexOf(separatorChar, baseEnd);
        }
        baseEnd++; // convert end index to interval boundary

        // Finally split according to our findings.
        if (baseBegin >= prefixLen) { // found separator after the prefix?
            final int parentEnd = lastIndexNot(path, separatorChar, baseBegin) + 1;
            result[0] = path.substring(0, parentEnd > prefixLen ? parentEnd : prefixLen);        // include separator, may produce separator only!
            result[1] = path.substring(baseBegin + 1, baseEnd);  // between separator and trailing separator
        } else { // no separator after prefix
            if (0 < prefixLen && prefixLen < baseEnd)       // prefix exists and we have more?
                result[0] = path.substring(0, prefixLen);    // prefix is parent
            else
                result[0] = null;                            // no parent
            result[1] = path.substring(prefixLen, baseEnd);
        }

        return result;
    }

    @SuppressWarnings("empty-statement")
    private static int lastIndexNot(String path, char separatorChar, int last) {
        while (path.charAt(last) == separatorChar && --last >= 0)
            ;
        return last;
    }

    /**
     * Returns the length of the file system prefix in {@code path}.
     * File system prefixes are:
     * <ol>
     * <li>A letter followed by a colon and an optional separator.
     *     On Windows, this is the notation for a drive.
     * <li>Two leading separators.
     *     On Windows, this is the notation for a UNC.
     * <li>A single leading separator.
     *     On Windows and POSIX, this is the notation for an absolute path.
     * </ol>
     * This method works identical on all platforms, so even if the separator
     * is {@code '/'}, two leading separators would be considered to
     * be a UNC and hence the return value would be {@code 2}.
     *
     * @param path The file system path.
     * @param separatorChar The file name separator character in {@code path}.
     * @return The number of characters in the prefix.
     * @throws NullPointerException If {@code path} is {@code null}.
     */
    private static int prefixLength(final String path, final char separatorChar) {
        final int pathLen = path.length();
        int len = 0; // default prefix length
        if (pathLen > 0 && path.charAt(0) == separatorChar) {
            len++; // leading separator or first character of a UNC.
        } else if (pathLen > 1 && path.charAt(1) == ':') {
            final char drive = path.charAt(0);
            if ('A' <= drive && drive <= 'Z'
                    || 'a' <= drive && drive <= 'z') { // US-ASCII letters only
                // Path is prefixed with drive, e.g. "C:\\Programs".
                len = 2;
            }
        }
        if (pathLen > len && path.charAt(len) == separatorChar)
            len++; // leading separator is considered part of prefix
        return len;
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
        a = getCanOrAbsFile(a);
        b = getCanOrAbsFile(b);
        return contains(a.getPath(), b.getPath());
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
        // Windows is just case preserving, all others are case sensitive.
        if (File.separatorChar == '\\') {
            a = a.toLowerCase();
            b = b.toLowerCase();
        }
        if (!b.startsWith(a)) {
            return false;
        }
        final int lengthA = a.length();
        final int lengthB = b.length();
        if (lengthA == lengthB) {
            return true;
        } else if (lengthA < lengthB) {
            return b.charAt(lengthA) == File.separatorChar;
        }
        return false;
    }
}
