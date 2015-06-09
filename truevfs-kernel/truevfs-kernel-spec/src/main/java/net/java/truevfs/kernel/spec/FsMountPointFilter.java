/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import net.java.truecommons.shed.Filter;

import java.net.URI;

import static net.java.truevfs.kernel.spec.FsNodeName.SEPARATOR_CHAR;

/**
 * A filter which accepts a given
 * {@linkplain FsMountPoint file system mount points} if its
 * {@linkplain FsMountPoint#toHierarchicalUri() hierarchical URI} matches
 * the configured prefix file system mount point.
 *
 * @author Christian Schlichtherle
 */
public final class FsMountPointFilter implements Filter<FsMountPoint> {

    private final String prefixScheme, prefixPath;
    private final int prefixPathLength;
    private final boolean prefixPathEndsWithSeparator;

    /**
     * Constructs a new file system mount point filter.
     *
     * @param prefix the prefix file system mount point used to filter the given
     *               file system mount points.
     */
    public FsMountPointFilter(final FsMountPoint prefix) {
        final URI prefixUri = prefix.toHierarchicalUri();
        this.prefixScheme = prefixUri.getScheme();
        this.prefixPath = prefixUri.getPath();
        this.prefixPathLength = prefixPath.length();
        this.prefixPathEndsWithSeparator =
                prefixPath.charAt(prefixPathLength - 1) == SEPARATOR_CHAR;
    }

    @Override
    public boolean accept(final FsMountPoint mountPoint) {
        final URI uri = mountPoint.toHierarchicalUri();
        final String uriPath;
        return uri.getScheme().equals(prefixScheme) &&
                (uriPath = uri.getPath()).startsWith(prefixPath) &&
                (prefixPathEndsWithSeparator ||
                        uriPath.length() == prefixPathLength ||
                        uriPath.charAt(prefixPathLength) == SEPARATOR_CHAR);
    }
}
