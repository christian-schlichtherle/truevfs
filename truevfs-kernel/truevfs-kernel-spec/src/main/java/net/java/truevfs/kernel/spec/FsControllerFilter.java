/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.net.URI;
import net.java.truecommons.shed.Filter;
import static net.java.truevfs.kernel.spec.FsNodeName.SEPARATOR_CHAR;

/**
 * Filters {@linkplain FsController file system controllers} so that the
 * {@linkplain FsMountPoint#toHierarchicalUri() hierarchical URI}
 * of the {@linkplain FsModel#getMountPoint() mount point} of their
 * {@linkplain FsController#getModel() file system model} must match the given
 * prefix.
 *
 * @see    FsManager#sync
 * @author Christian Schlichtherle
 */
public final class FsControllerFilter implements Filter<FsController> {

    private final String scheme, path;
    private final int pathLength;
    private final boolean pathEndsWithSeparator;

    /**
     * Constructs a new file system controller filter.
     *
     * @param prefix the prefix of the mount point used to filter file system
     *        controllers.
     */
    public FsControllerFilter(final FsMountPoint prefix) {
        final URI p = prefix.toHierarchicalUri();
        this.scheme = p.getScheme();
        this.path = p.getPath();
        this.pathLength = path.length();
        this.pathEndsWithSeparator = SEPARATOR_CHAR == path.charAt(pathLength - 1);
    }

    @Override
    public boolean accept(final FsController controller) {
        final URI mp = controller.getModel().getMountPoint().toHierarchicalUri();
        final String path;
        return mp.getScheme().equals(scheme)
                && (path = mp.getPath()).startsWith(this.path)
                && (pathEndsWithSeparator
                    || path.length() == pathLength
                    || SEPARATOR_CHAR == path.charAt(pathLength));
    }
}
