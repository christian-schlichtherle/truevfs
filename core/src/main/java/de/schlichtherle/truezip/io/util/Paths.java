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

package de.schlichtherle.truezip.io.util;

import java.io.File;

/**
 * Static utility methods for path names.
 * In order to enhance interoperability, the methods in this class always
 * detect Windows drives (&quot;[a-zA-Z]:&quot;) and UNCs (&quot;\\&quot;) in
 * a path name, even on non-Windows platforms.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Paths {

    /**
     * Equivalent to {@link #normalize(String, char)
     * normalize(path, File.separatorChar)}.
     */
    public static String normalize(final String path) {
        return normalize(path, File.separatorChar);
    }

    /**
     * Removes all redundant separators, dot directories
     * ({@code &quot;.&quot;}) and dot-dot directories
     * ({@code &quot;..&quot;}) from the path and returns the result.
     * An empty path results in {@code &quot;.&quot;}.
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
     * Removes all redundant separators, dot directories
     * ({@code &quot;.&quot;}) and dot-dot directories
     * ({@code &quot;..&quot;}) from the path and collects the result
     * in a string builder.
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

    /** You cannot instantiate this class. */
    protected Paths() {
    }
}
