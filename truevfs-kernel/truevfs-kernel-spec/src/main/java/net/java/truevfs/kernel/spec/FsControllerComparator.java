/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares {@linkplain FsController file system controllers} in reverse order of the
 * {@linkplain FsMountPoint#toHierarchicalUri() hierarchical URI} of the
 * {@linkplain FsModel#getMountPoint() mount point} of their {@linkplain FsController#getModel() file system model}.
 * When applied to a list of file system controllers for {@linkplain FsController#sync sync()}ing, this ordering ensures
 * that any archive file system gets {@code sync()}ed before their parent file system so that all file systems reflect
 * all changes once the list has been completely processed.
 *
 * @author Christian Schlichtherle
 */
public class FsControllerComparator
implements Comparator<FsController>, Serializable {

    private static final long serialVersionUID = 0L;

    @Override
    public int compare(FsController o1, FsController o2) {
        return o2.getModel().getMountPoint().toHierarchicalUri().compareTo(
                o1.getModel().getMountPoint().toHierarchicalUri());
    }
}
