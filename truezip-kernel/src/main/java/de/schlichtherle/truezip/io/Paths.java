/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Locale;

/**
 * Provides static utility methods for path names.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class Paths {

    /** You cannot instantiate this class. */
    private Paths() {
    }

    /**
     * Equivalent to
     * {@code new Normalizer(separatorChar).}{@link Normalizer#normalize(String)}.
     */
    public static String normalize(String path, char separatorChar) {
        return new Normalizer(separatorChar).normalize(path);
    }

    /** A normalizer for path names. */
    public static class Normalizer {
        private final char separatorChar;
        private String path;
        private final StringBuilder buffer;

        public Normalizer(final char separatorChar) {
            this.separatorChar = separatorChar;
            buffer = new StringBuilder();
        }

        /**
         * Removes all redundant separators, dot directories ({@code "."}) and
         * dot-dot directories ({@code ".."}) from the given path name and
         * returns the result.
         * If present, a single trailing separator character is retained,
         * except after a dot-dot directory which couldn't get erased.
         * A resulting single dot-directory is truncated to an empty path.
         * <p>
         * On Windows, a path may be prefixed by a drive letter followed by a
         * colon.
         * On all platforms, a path may be prefixed by two leading separators
         * to indicate a UNC, although this is currently only supported on
         * Windows.
         *
         * @param  path the non-{@code null} path name to normalize.
         * @return {@code path} if it was already in normalized form.
         *         Otherwise, a new string with the normalized form of the
         *         given path name.
         */
        public String normalize(final String path) {
            final int prefixLen = prefixLength(path, separatorChar);
            final int pathLen = path.length();
            this.path = path.substring(prefixLen, pathLen);
            buffer.setLength(0);
            buffer.ensureCapacity(pathLen);
            normalize(0, pathLen - prefixLen);
            buffer.insert(0, path.substring(0, prefixLen));
            int bufferLen = buffer.length();
            String result;
            if (    pathLen > 0 && path.charAt(pathLen - 1) == separatorChar ||
                    pathLen > 1 && path.charAt(pathLen - 2) == separatorChar &&
                                   path.charAt(pathLen - 1) == '.') {
                slashify();
                bufferLen = buffer.length();
            }
            if (bufferLen == path.length()) {
                assert path.equals(buffer.toString());
                result = path;
            } else {
                result = buffer.toString();
                if (path.startsWith(result))
                    result = path.substring(0, bufferLen);
            }
            assert !result.equals(path) || result == path; // postcondition
            return result;
        }

        /**
         * This is a recursive call: The top level call should provide
         * {@code 0} as the {@code skip} parameter, the length
         * of the path as the {@code end} parameter and an empty string
         * buffer as the {@code result} parameter.
         *
         * @param  collapse the number of adjacent <i>dir/..</i> segments in
         *         the path to collapse.
         *         This value must not be negative.
         * @param  end the current position in {@code path}.
         *         Only the string to the left of this index is considered.
         *         If not positive, nothing happens.
         * @return The number of adjacent segments in the path which have
         *         <em>not</em> been collapsed at this position.
         */
        private int normalize(final int collapse, final int end) {
            assert collapse >= 0;
            if (0 >= end)
                return collapse;
            final int next = path.lastIndexOf(separatorChar, end - 1);
            final String base = path.substring(next + 1, end);
            int notCollapsed;
            if (0 >= base.length() || ".".equals(base)) {
                return normalize(collapse, next);
            } else if ("..".equals(base)) {
                notCollapsed = normalize(collapse + 1, next) - 1;
                if (0 > notCollapsed)
                    return 0;
            } else if (0 < collapse) {
                notCollapsed = normalize(collapse - 1, next);
                slashify();
                return notCollapsed;
            } else {
                assert 0 == collapse;
                notCollapsed = normalize(0, next);
                assert 0 == notCollapsed;
            }
            slashify();
            buffer.append(base);
            return notCollapsed;
        }

        private void slashify() {
            final int bufferLen = buffer.length();
            if (bufferLen > 0 && buffer.charAt(bufferLen - 1) != separatorChar)
                buffer.append(separatorChar);
        }
    } // class Normalizer

    /**
     * Cuts off any separator characters at the end of the given path name,
     * unless the path name contains of only separator characters, in which
     * case a single separator character is retained to denote the root
     * directory.
     *
     * @param  path The path name to chop.
     * @param  separatorChar The file name separator character.
     * @return {@code path} if it's a path name without trailing separators
     *         or contains the separator character only.
     *         Otherwise, the substring until the first of at least one
     *         separating characters is returned.
     */
    public static String cutTrailingSeparators(
            final String path,
            final char separatorChar) {
        int i = path.length();
        if (0 >= i || separatorChar != path.charAt(--i))
            return path;
        while (0 < i && separatorChar == path.charAt(--i)) {
        }
        return path.substring(0, ++i);
    }

    /**
     * Equivalent to
     * <code>return new {@link Splitter#Paths.Splitter(char, boolean) Splitter(separatorChar, keepTrailingSeparator)}.{@link Splitter#split(String) split(path)};</code>.
     */
    public static Splitter split(   String path,
                                    char separatorChar,
                                    boolean keepTrailingSeparator) {
        return new Splitter(separatorChar, keepTrailingSeparator).split(path);
    }

    /** Splits a given path name into its parent path name and member name. */
    public static class Splitter {
        private final char separatorChar;
        private final int fixum;
        private @CheckForNull String parentPath;
        private String memberName;

        /**
         * Constructs a new splitter.
         *
         * @param separatorChar the file name separator character.
         * @param keepTrailingSeparator whether or not a parent path name
         *        should have a single trailing separator if present in the
         *        original path name.
         */
        public Splitter(final char separatorChar,
                        final boolean keepTrailingSeparator) {
            this.separatorChar = separatorChar;
            this.fixum = keepTrailingSeparator ? 2 : 1;
        }

        /**
         * Splits the given path name into its parent path name and member name,
         * recognizing platform specific file system roots.
         * The returned array will hold the following strings:
         * <ol>
         * <li>At index zero: The parent path name with a single trailing
         *     {@code separatorChar} or {@code null} if the path name does not
         *     specify a parent.</li>
         * <li>At index one: The member name without a {@code separatorChar}.</li>
         * </ol>
         *
         * @param  path the name of the path which's parent path name and
         *         member name are to be returned.
         * @return {@code this}
         */
        public Splitter split(final String path) {
            final int prefixLength = prefixLength(path, separatorChar);
            int memberEnd = path.length() - 1;
            if (memberEnd < prefixLength) {
                parentPath = null;
                memberName = "";
                return this;
            }
            memberEnd = lastIndexNot(path, separatorChar, memberEnd);
            int memberBegin = path.lastIndexOf(separatorChar, memberEnd);
            memberEnd++;
            if (prefixLength <= memberBegin) {
                final int parentEnd = lastIndexNot(path, separatorChar, memberBegin);
                parentPath = path.substring(0, prefixLength <= parentEnd ? parentEnd + fixum : prefixLength);
                memberName = path.substring(memberBegin + 1, memberEnd);
            } else if (0 < prefixLength && prefixLength <= memberEnd) {
                parentPath = path.substring(0, prefixLength);
                memberName = path.substring(prefixLength, memberEnd);
            } else if (prefixLength <= memberEnd) {
                parentPath = null;
                memberName = path.substring(memberBegin + 1, memberEnd);
            } else {
                parentPath = null;
                memberName = "";
            }
            return this;
        }

        private static int lastIndexNot(String path, char separatorChar, int last) {
            while (path.charAt(last) == separatorChar && --last >= 0) {
            }
            return last;
        }

        /** Returns the parent path name. */
        @Nullable
        public String getParentPath() {
            return parentPath;
        }

        /** Returns the member name. */
        public String getMemberName() {
            return memberName;
        }
    } // class Splitter

    /**
     * Returns {@code true} iff the given path name refers to the root
     * directory, i.e. if it's empty.
     */
    public static boolean isRoot(String path) {
        return path.isEmpty();
    }

    /**
     * Returns {@code true} iff the given path name is absolute.
     * Windows drives and UNC's are always recognized by this method, even
     * on non-Windows platforms in order to ease interoperability.
     *
     * @param  path the path name to test.
     * @param  separatorChar the file name separator character.
     * @return Whether or not path is prefixed and the prefix ends with a
     *         separator character.
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
     * @param  path The file system path.
     * @param  separatorChar The file name separator character.
     * @return The number of characters in the prefix.
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
            len++; // next separator is considered part of prefix
        return len;
    }

    /**
     * Returns true if and only if the path name represented by {@code a}
     * contains the path name represented by {@code b}.
     *
     * @param a A non-{@code null} {@link String} reference.
     * @param b A non-{@code null} {@link String} reference.
     * @param separatorChar The file name separator character.
     */
    public static boolean contains(String a, String b, char separatorChar) {
        // Windows is just case preserving, all others are case sensitive.
        if (separatorChar == '\\') {
            a = a.toLowerCase(Locale.ENGLISH);
            b = b.toLowerCase(Locale.ENGLISH);
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
