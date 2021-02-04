/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.shed;

import java.util.Optional;

/**
 * Splits a given path name into its parent path name and member name,
 * whereby platform specific file system roots get recognized.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class PathSplitter {

    private final char separatorChar;
    private final int fixum;
    private Optional<String> parentPath = Optional.empty();
    private Optional<String> memberName = Optional.empty();

    /**
     * Constructs a new splitter.
     *
     * @param separatorChar         the file name separator character.
     * @param keepTrailingSeparator whether or not a parent path name
     *                              should have a single trailing separator if present in the
     *                              original path name.
     */
    public PathSplitter(final char separatorChar, final boolean keepTrailingSeparator) {
        this.separatorChar = separatorChar;
        this.fixum = keepTrailingSeparator ? 1 : 0;
    }

    /**
     * Splits the given path name into its parent path name and member name.
     * Platform specific file system roots get recognized, e.g. {@code C:\} on
     * Windows.
     *
     * @param path the path name which's parent path name and member name
     *             are to be returned.
     * @return {@code this}
     */
    public PathSplitter split(final String path) {
        final int prefixLen = Paths.prefixLength(path, separatorChar, false);
        int memberEnd = path.length() - 1;
        if (prefixLen > memberEnd) {
            parentPath = Optional.empty();
            memberName = Optional.of("");
            return this;
        }
        memberEnd = lastIndexNot(path, separatorChar, memberEnd);
        final int memberInd = path.lastIndexOf(separatorChar, memberEnd) + 1;
        memberEnd++;
        final int parentEnd;
        if (prefixLen >= memberEnd) {
            parentPath = Optional.empty();
            memberName = Optional.of("");
        } else if (prefixLen >= memberInd) {
            parentPath = 0 >= prefixLen ? Optional.empty() : Optional.of(path.substring(0, prefixLen));
            memberName = Optional.of(path.substring(prefixLen, memberEnd));
        } else if (prefixLen >= (parentEnd = lastIndexNot(path, separatorChar, memberInd - 1) + 1)) {
            parentPath = Optional.of(path.substring(0, prefixLen));
            memberName = Optional.of(path.substring(memberInd, memberEnd));
        } else {
            parentPath = Optional.of(path.substring(0, parentEnd + fixum));
            memberName = Optional.of(path.substring(memberInd, memberEnd));
        }
        return this;
    }

    private static int lastIndexNot(String path, char separatorChar, int last) {
        while (separatorChar == path.charAt(last) && --last >= 0) {
        }
        return last;
    }

    /**
     * Returns the parent path name.
     *
     * @return The parent path name.
     */
    public Optional<String> getParentPath() {
        return parentPath;
    }

    /**
     * Returns the member name.
     *
     * @return The member name.
     */
    public String getMemberName() {
        return memberName.get();
    }
}
