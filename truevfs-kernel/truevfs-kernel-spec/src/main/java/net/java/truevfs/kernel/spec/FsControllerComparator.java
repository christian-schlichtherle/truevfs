/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.Comparator;

/**
 * Compares {@linkplain FsController file system controllers} in reverse order
 * of the
 * {@linkplain FsMountPoint#getHierarchicalUri() hierarchical URI}
 * of the {@linkplain FsModel#getMountPoint() mount point} of their
 * {@linkplain FsController#getModel() file system model}.
 * When applied to a list of file system controllers for
 * {@linkplain FsController#sync sync()}ing, this ordering ensures that any
 * archive file system gets {@code sync()}ed before their parent file system so
 * that all file systems reflect all changes once the list has been completely
 * processed.
 * 
 * @see    FsControllerStream
 * @author Christian Schlichtherle
 */
public class FsControllerComparator implements Comparator<FsController> {

    @Override
    public int compare(FsController o1, FsController o2) {
        return o2.getModel().getMountPoint().getHierarchicalUri().compareTo(
                o1.getModel().getMountPoint().getHierarchicalUri());
    }
}
