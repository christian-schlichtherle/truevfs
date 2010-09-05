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

/**
 * Provides static utility methods for path names.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Paths {

    /** You cannot instantiate this class. */
    Paths() {
    }

    /**
     * Equivalent to
     * {@code new Normalizer(separatorChar).}{@link Normalizer#normalize(String)}.
     */
    public static String normalize(String path, char separatorChar) {
        return new Normalizer(separatorChar).normalize(path);
    }

    public static class Normalizer {
        private final char separatorChar;
        private String path;
        private final StringBuilder builder;

        public Normalizer(final char separatorChar) {
            this.separatorChar = separatorChar;
            builder = new StringBuilder();
        }

        /**
         * Removes all redundant separators, dot directories ({@code "."}) and
         * dot-dot directories ({@code ".."}) from the path name and returns
         * the result.
         * Trailing separator characters are removed and a single {@code "."}
         * gets truncated to an empty path.
         * <p>
         * On Windows, a path may be prefixed by a drive letter followed by a
         * colon.
         * On all platforms, a path may be prefixed by two leading separators
         * to indicate a UNC, although this is currently only supported on
         * Windows.
         *
         * @param  path the path name to normalize.
         * @return {@code path} if it was already in normalized form.
         *         Otherwise, a new String with the normalized form of the
         *         given path name.
         * @throws NullPointerException if {@code path} is {@code null}.
         */
        public String normalize(final String path) {
            final int prefixLength = prefixLength(path, separatorChar);
            final int pathLength = path.length();
            this.path = path.substring(prefixLength, pathLength);
            builder.setLength(0);
            normalize(0, pathLength - prefixLength);
            builder.insert(0, path.substring(0, prefixLength));
            final int builderLength = builder.length();
            String result;
            if (builderLength == path.length()) {
                assert path.equals(builder.toString());
                result = path;
            } else {
                result = builder.toString();
                if (path.startsWith(result))
                    result = path.substring(0, builderLength);
            }
            assert !result.equals(path) || result == path; // postcondition
            return result;
        }

        private int normalize(final int skip, final int end) {
            assert skip >= 0;
            if (end <= 0)
                return 0;
            final int next = path.lastIndexOf(separatorChar, end - 1);
            final String base = path.substring(next + 1, end);
            final int skipped;
            if (base.length() == 0 || ".".equals(base)) {
                return normalize(skip, next);
            } else if ("..".equals(base)) {
                final int toSkip = skip + 1;
                skipped = normalize(toSkip, next);
                assert skipped <= toSkip;
                if (skipped == toSkip)
                    return skip;
            } else if (skip > 0) {
                return normalize(skip - 1, next) + 1;
            } else {
                assert skip == 0;
                skipped = normalize(skip, next);
                assert skipped == 0;
            }
            final int builderLength = builder.length();
            if (builderLength > 0) {
                assert builder.charAt(builderLength - 1) != separatorChar;
                builder.append(separatorChar);
            }
            builder.append(base);
            return skipped;
        }
    } // class Normalizer

    /**
     * Cuts off any separator characters at the end of the given path name,
     * unless the path name contains of only separator characters, in which
     * case a single separator character is retained to denote the root
     * directory.
     *
     * @param path The path name to chop.
     * @param separatorChar The file name separator character.
     * @return {@code path} if it's a path name without trailing separators
     *         or contains the separator character only.
     *         Otherwise, the substring until the first of at least one
     *         separating characters is returned.
     * @throws NullPointerException If {@code path} is {@code null}.
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
     * Equivalent to
     * {@code return new Splitter(separatorChar).{@link Splitter#split(String) split(path)};}.
     */
    public static String[] split(String path, char separatorChar) {
        return new Splitter(separatorChar).split(path);
    }

    public static class Splitter {
        private final char separatorChar;
        private final String[] result = new String[2];

        public Splitter(final char separatorChar) {
            this.separatorChar = separatorChar;
        }

        /**
         * Splits a path name into its parent path name and its base name,
         * recognizing platform specific file system roots.
         * The returned array will hold the following strings:
         * <ol>
         * <li>At index zero: The parent path name or {@code null} if the
         *     path name does not specify a parent.
         *     This compares equal with {@link java.io.File#getParent()}.</li>
         * <li>At index one: The base name.
         *     This compares equal with {@link java.io.File#getName()}.</li>
         * </ol>
         *
         * @param path The name of the path which's parent path name and base name
         *        are to be returned.
         * @return An array of at least two string elements to hold the resul
         *         of the operation.
         * @throws NullPointerException If {@code path} is {@code null}.
         */
        public String[] split(final String path) {
            final int prefixLength = prefixLength(path, separatorChar);
            // Skip any trailing separators and look for the previous separator.
            int baseBegin = -1;
            int baseEnd = path.length() - 1;
            if (prefixLength <= baseEnd) {
                baseEnd = lastIndexNot(path, separatorChar, baseEnd);
                baseBegin = path.lastIndexOf(separatorChar, baseEnd);
            }
            baseEnd++; // convert end index to interval boundary
            // Finally split according to our findings.
            if (baseBegin >= prefixLength) { // found separator after the prefix?
                final int parentEnd = lastIndexNot(path, separatorChar, baseBegin) + 1;
                result[0] = path.substring(0, parentEnd > prefixLength ? parentEnd : prefixLength);        // include separator, may produce separator only!
                result[1] = path.substring(baseBegin + 1, baseEnd);  // between separator and trailing separator
            } else { // no separator after prefix
                if (0 < prefixLength && prefixLength < baseEnd)       // prefix exists and we have more?
                    result[0] = path.substring(0, prefixLength);    // prefix is parent
                else
                    result[0] = null;                            // no parent
                result[1] = path.substring(prefixLength, baseEnd);
            }
            return result;
        }

        public String getParentPath() {
            return result[0];
        }

        public String getBaseName() {
            return result[1];
        }
    }


    @SuppressWarnings("empty-statement")
    private static int lastIndexNot(String path, char separatorChar, int last) {
        while (path.charAt(last) == separatorChar && --last >= 0)
            ;
        return last;
    }

    /**
     * Returns {@code true} iff the given path name is absolute.
     * Windows drives and UNC's are always recognized by this method, even
     * on non-Windows platforms in order to ease interoperability.
     *
     * @param path The path name to test.
     * @param separatorChar The file name separator character.
     * @return Whether or not path is prefixed and the prefix ends with a
     *         separator character.
     * @throws NullPointerException If {@code path} is {@code null}.
     */
    public static boolean isAbsolute(String path, char separatorChar) {
        final int prefixLen = prefixLength(path, separatorChar);
        return prefixLen > 0 && path.charAt(prefixLen - 1) == separatorChar;
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
     * @param separatorChar The file name separator character.
     * @return The number of characters in the prefix.
     * @throws NullPointerException If {@code path} is {@code null}.
     */
    private static int prefixLength(final String path, final char separatorChar) {
        final int pathLength = path.length();
        int len = 0; // default prefix length
        if (pathLength > 0 && path.charAt(0) == separatorChar) {
            len++; // leading separator or first character of a UNC.
        } else if (pathLength > 1 && path.charAt(1) == ':') {
            final char drive = path.charAt(0);
            if ('A' <= drive && drive <= 'Z'
                    || 'a' <= drive && drive <= 'z') { // US-ASCII letters only
                // Path is prefixed with drive, e.g. "C:\\Programs".
                len = 2;
            }
        }
        if (pathLength > len && path.charAt(len) == separatorChar)
            len++; // leading separator is considered part of prefix
        return len;
    }

    /**
     * Returns true if and only if the path name represented by {@code a}
     * contains the path name represented by {@code b}.
     *
     * @param a A non-{@code null} {@link String} reference.
     * @param b A non-{@code null} {@link String} reference.
     * @param separatorChar The file name separator character.
     * @throws NullPointerException If any parameter is {@code null}.
     */
    public static boolean contains(String a, String b, char separatorChar) {
        // Windows is just case preserving, all others are case sensitive.
        if (separatorChar == '\\') {
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
            return b.charAt(lengthA) == separatorChar;
        }
        return false;
    }
}
