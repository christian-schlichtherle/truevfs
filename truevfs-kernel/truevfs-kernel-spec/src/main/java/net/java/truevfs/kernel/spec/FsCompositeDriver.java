/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.ServiceConfigurationError;
import javax.annotation.CheckForNull;
import net.java.truecommons.shed.ImplementationsShouldExtend;

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
@ImplementationsShouldExtend(FsAbstractCompositeDriver.class)
public interface FsCompositeDriver
extends FsControllerFactory<FsManagerWithControllerFactory> {

    /**
     * {@inheritDoc}
     * <p>
     * The file system controller gets created by using a
     * {@link FsDriver file system driver} which gets looked up by querying the
     * scheme of the mount point of the given file system model with the
     * expression {@code model.getMountPoint().getScheme()}.
     *
     * @throws ServiceConfigurationError if no appropriate file system driver
     *         is known for the scheme of the given mount point.
     */
    @Override
    FsController newController(
            FsManagerWithControllerFactory context,
            FsModel model,
            @CheckForNull FsController parent)
    throws ServiceConfigurationError;
}
