/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import net.java.truecommons.shed.Filter;

import java.net.URI;

import static net.java.truevfs.kernel.spec.FsNodeName.SEPARATOR_CHAR;

/**
 * A filter which accepts a given file system
 * {@linkplain FsMountPoint mount point} if its
 * {@linkplain FsMountPoint#toHierarchicalUri() hierarchical URI} matches
 * the configured prefix mount point.
 *
 * @author Christian Schlichtherle
 */
public final class FsPrefixMountPointFilter implements Filter<FsMountPoint> {

    private final String prefixScheme, prefixPath;
    private final int prefixPathLength;
    private final boolean prefixPathEndsWithSeparator;

    private FsPrefixMountPointFilter(final FsMountPoint prefix) {
        final URI prefixUri = prefix.toHierarchicalUri();
        this.prefixScheme = prefixUri.getScheme();
        this.prefixPath = prefixUri.getPath();
        this.prefixPathLength = prefixPath.length();
        this.prefixPathEndsWithSeparator =
                prefixPath.charAt(prefixPathLength - 1) == SEPARATOR_CHAR;
    }

    /** Returns a prefix mount point filter for the given prefix mount point. */
    public static FsPrefixMountPointFilter forPrefix(FsMountPoint prefix) {
        return new FsPrefixMountPointFilter(prefix);
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
