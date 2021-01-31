/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import javax.annotation.CheckForNull;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.function.Supplier;

/**
 * An abstract composite driver.
 * This class provides an implementation of {@link #newController} which uses
 * the file system driver map returned by {@link #get()} to lookup the
 * appropriate driver for the scheme of any given mount point.
 * <p>
 * Subclasses should be immutable, too.
 *
 * @author Christian Schlichtherle
 */
public abstract class FsAbstractCompositeDriver implements FsCompositeDriver, Supplier<Map<FsScheme, FsDriver>> {

    @Override
    public final FsModel newModel(
            final FsManager context,
            final FsMountPoint mountPoint,
            final FsModel parent) {
        assert null == parent
                    ? null == mountPoint.getParent()
                    : parent.getMountPoint().equals(mountPoint.getParent());
        return driver(mountPoint).newModel(context, mountPoint, parent);
    }

    @Override
    public final FsController newController(
            final FsManager context,
            final FsModel model,
            final @CheckForNull FsController parent)
    throws ServiceConfigurationError {
        assert null == parent
                    ? null == model.getParent()
                    : parent.getModel().equals(model.getParent());
        return driver(model.getMountPoint()).newController(context, model, parent);
    }

    private FsDriver driver(final FsMountPoint mountPoint) {
        final FsScheme scheme = mountPoint.getScheme();
        final FsDriver driver = get().get(scheme);
        if (null == driver)
            throw new ServiceConfigurationError(scheme
                    + " (Unknown file system scheme! May be the class path doesn't contain the respective driver module or it isn't set up correctly?)");
        return driver;
    }
}
