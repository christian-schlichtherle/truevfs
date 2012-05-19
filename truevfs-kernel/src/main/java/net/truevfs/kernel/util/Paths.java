/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util;

import java.io.File;
import java.util.Locale;
import javax.annotation.concurrent.Immutable;

/**
 * Static utility methods for path names.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class Paths {

    /* Can't touch this - hammer time! */
    private Paths() { }

    /**
     * Equivalent to
     * <code>new {@link PathNormalizer#PathNormalizer(char) PathNormalizer(separatorChar)}&#x2e;{@link PathNormalizer#normalize(String) normalize(path)}</code>.
     */
    public static String normalize(String path, char separatorChar) {
        return new PathNormalizer(separatorChar).normalize(path);
    }

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
     * <code>new {@link PathSplitter#PathSplitter(char, boolean) PathSplitter(separatorChar, keepTrailingSeparator)}&#x2e;{@link PathSplitter#split(String) split(path)}</code>.
     */
    public static PathSplitter split(
            String path,
            char separatorChar,
            boolean keepTrailingSeparator) {
        return new PathSplitter(separatorChar, keepTrailingSeparator).split(path);
    }

    /**
     * Returns {@code true} iff the given path name refers to the root
     * directory, i.e. if it's empty.
     */
    public static boolean isRoot(String path) {
        return path.isEmpty();
    }

    /**
     * Returns {@code true} iff the given path name is absolute.
     * Windows drives and UNC's are recognized if and only if this JVM is
     * running on Windows.
     *
     * @param  path the path name to test.
     * @param  separatorChar the file name separator character.
     * @return Whether or not path is prefixed and the prefix ends with a
     *         separator character.
     */
    public static boolean isAbsolute(String path, char separatorChar) {
        final int prefixLen = prefixLength(path, separatorChar, false);
        return 0 < prefixLen && separatorChar == path.charAt(prefixLen - 1);
    }

    /**
     * Returns the length of the file system prefix in {@code path}.
     * File system prefixes are:
     * <ol>
     * <li>A letter followed by a colon and an optional separator.
     *     This indicates a Windows Drive and is only recognized if this
     *     JVM is running on Windows.
     * <li>Two leading separators.
     *     This indicates an authority and is only recognized if this JVM is
     *     running on Windows.
     *     If {@code inclUNC} is {@code true}, then the next two segments
     *     following the two leading separators are accounted for in the prefix
     *     length as the UNC host and share name.
     * <li>A single leading separator.
     *     On Windows and POSIX, this is the notation for an absolute path.
     *     This is recognized on any OS.
     * </ol>
     *
     * @param  path The file system path.
     * @param  separatorChar The file name separator character.
     * @param  inclUNC whether or not an authority or a UNC host and
     *         share name should get accounted for in the prefix length.
     * @return The number of characters in the prefix.
     */
    public static int prefixLength(
            final String path,
            final char separatorChar,
            final boolean inclUNC) {
        final int pathLen = path.length();
        if (pathLen <= 0)
            return 0;
        char c = path.charAt(0);
        if ('\\' == File.separatorChar) {
            if (2 <= pathLen
                    && ':' == path.charAt(1)
                    && ('a' <= c && c <= 'z' || 'A' <= c && c <= 'Z')) {
                    // Windows Drive.
                    return 3 <= pathLen && separatorChar == path.charAt(2) ? 3 : 2;
            } else if (separatorChar == c) {
                if (2 <= pathLen && separatorChar == path.charAt(1)) {
                    // Windows UNC.
                    if (!inclUNC)
                        return 2;
                    final int i = path.indexOf(separatorChar, 2) + 1;
                    if (0 == i)
                        return pathLen;
                    // UNC host name.
                    final int j = path.indexOf(separatorChar, i) + 1;
                    if (0 == j)
                        return pathLen;
                    // UNC share name.
                    return j;
                } else {
                    // Absolute path.
                    return 1;
                }
            } else {
                // Relative path.
                return 0;
            }
        } else {
            // Absolute or relative path.
            return separatorChar == c ? 1 : 0;
        }
    }

    /**
     * Returns true if and only if the path name represented by {@code a}
     * contains the path name represented by {@code b}.
     * If and only if this JVM is running on Windows, the comparison is case
     * insensitive with respect to the
     * {@link Locale#getDefault() default locale}.
     *
     * @param a A non-{@code null} {@link String} reference.
     * @param b A non-{@code null} {@link String} reference.
     * @param separatorChar The file name separator character.
     */
    public static boolean contains(String a, String b, char separatorChar) {
        // Windows is just case preserving, all others are case sensitive.
        if ('\\' == File.separatorChar) {
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
