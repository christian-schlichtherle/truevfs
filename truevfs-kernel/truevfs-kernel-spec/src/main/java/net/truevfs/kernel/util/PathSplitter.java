/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Splits a given path name into its parent path name and member name,
 * whereby platform specific file system roots get recognized.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class PathSplitter {
    
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
    public PathSplitter(final char separatorChar, final boolean keepTrailingSeparator) {
        this.separatorChar = separatorChar;
        this.fixum = keepTrailingSeparator ? 1 : 0;
    }

    /**
     * Splits the given path name into its parent path name and member name.
     * Platform specific file system roots get recognized, e.g. {@code C:\} on
     * Windows.
     *
     * @param  path the path name which's parent path name and member name
     *         are to be returned.
     * @return {@code this}
     */
    public PathSplitter split(final String path) {
        final int prefixLen = Paths.prefixLength(path, separatorChar, false);
        int memberEnd = path.length() - 1;
        if (prefixLen > memberEnd) {
            parentPath = null;
            memberName = "";
            return this;
        }
        memberEnd = lastIndexNot(path, separatorChar, memberEnd);
        final int memberInd = path.lastIndexOf(separatorChar, memberEnd) + 1;
        memberEnd++;
        final int parentEnd;
        if (prefixLen >= memberEnd) {
            parentPath = null;
            memberName = "";
        } else if (prefixLen >= memberInd) {
            parentPath = 0 >= prefixLen ? null : path.substring(0, prefixLen);
            memberName = path.substring(prefixLen, memberEnd);
        } else if (prefixLen >= (parentEnd = lastIndexNot(path, separatorChar, memberInd - 1) + 1)) {
            parentPath = path.substring(0, prefixLen);
            memberName = path.substring(memberInd, memberEnd);
        } else {
            parentPath = path.substring(0, parentEnd + fixum);
            memberName = path.substring(memberInd, memberEnd);
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
    @Nullable
    public String getParentPath() {
        return parentPath;
    }

    /**
     * Returns the member name.
     * 
     * @return The member name.
     */
    public String getMemberName() {
        return memberName;
    }
}
