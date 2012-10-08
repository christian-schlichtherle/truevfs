/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.ServiceConfigurationError;
import javax.annotation.CheckForNull;

/**
 * Queries the scheme of the mount point of the given file system model in
 * order to lookup the appropriate file system driver which is then used to
 * create the requested thread-safe file system controller.
 * <p>
 * Implementations should be immutable.
 *
 * @see    FsDriver
 * @author Christian Schlichtherle
 */
public interface FsMetaDriver {

    /**
     * Returns a new thread-safe file system controller for the mount point of
     * the given file system model.
     * The file system controller gets created by using a
     * {@link FsDriver file system driver} which gets looked up by querying the
     * scheme of the mount point of the given file system model with the
     * expression {@code model.getMountPoint().getScheme()}.
     * <p>
     * When called, you may assert the following precondition:
     * <pre>{@code
     * assert null == parent
     *         ? null == model.getParent()
     *         : parent.getModel().equals(model.getParent())
     * }</pre>
     * 
     * @param  manager the manager of the new file system controller.
     * @param  model the file system model.
     * @param  parent the nullable parent file system controller.
     * @return A new thread-safe file system controller for the mount point of
     *         the given file system model.
     * @throws ServiceConfigurationError if no appropriate file system driver
     *         is known for the scheme of the given mount point.
     * @see    FsDriver#newController
     */
    FsController newController(
            FsManager manager,
            FsModel model,
            @CheckForNull FsController parent)
    throws ServiceConfigurationError;
}