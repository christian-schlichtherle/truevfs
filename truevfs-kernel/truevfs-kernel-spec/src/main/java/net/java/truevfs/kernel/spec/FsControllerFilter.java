/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
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

    private final String ps, pp;
    private final int ppl;
    private final boolean pps;

    /**
     * Constructs a new file system controller filter.
     *
     * @param prefix the prefix of the mount point used to filter file system
     *        controllers.
     */
    public FsControllerFilter(final FsMountPoint prefix) {
        final URI p = prefix.toHierarchicalUri();
        this.ps = p.getScheme();
        this.pp = p.getPath();
        this.ppl = pp.length();
        this.pps = SEPARATOR_CHAR == pp.charAt(ppl - 1);
    }

    @Override
    public boolean accept(final FsController controller) {
        final URI mp = controller.getModel().getMountPoint().toHierarchicalUri();
        final String mpp;
        return mp.getScheme().equals(ps)
                && (mpp = mp.getPath()).startsWith(pp)
                && (pps
                    || mpp.length() == ppl
                    || SEPARATOR_CHAR == mpp.charAt(ppl));
    }
}
