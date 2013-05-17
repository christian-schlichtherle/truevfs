/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import javax.annotation.CheckForNull;

/**
 * A factory for {@linkplain FsModel file system models}.
 * <p>
 * Implementations need to be safe for multithreading.
 *
 * @param  <Context> The type of the calling context.
 * @since  TrueVFS 0.11
 * @author Christian Schlichtherle
 */
public interface FsModelFactory<Context> {

    /**
     * Returns a new thread-safe file system model for the given mount point.
     * This is a pure function without side effects.
     * <p>
     * When called, you may assert the following precondition:
     * <pre>{@code
     * assert null == parent
     *         ? null == mountPoint.getParent()
     *         : parent.getMountPoint().equals(mountPoint.getParent());
     * }</pre>
     *
     * @param context the calling context.
     * @param mountPoint the mount point of the file system.
     * @param parent the nullable parent file system model.
     * @return A new thread-safe file system model for the given mount point.
     */
    FsModel newModel(
            Context context,
            FsMountPoint mountPoint,
            @CheckForNull FsModel parent);
}
