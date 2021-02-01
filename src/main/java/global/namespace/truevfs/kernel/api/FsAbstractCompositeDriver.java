/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api;

import java.util.Map;
import java.util.Optional;
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
public abstract class FsAbstractCompositeDriver
        implements FsCompositeDriver, Supplier<Map<FsScheme, ? extends FsDriver>> {

    @Override
    public final FsModel newModel(
            final FsManager context,
            final FsMountPoint mountPoint,
            final Optional<? extends FsModel> parent) {
        assert mountPoint.getParent().equals(parent.map(FsModel::getMountPoint));
        return driver(mountPoint).newModel(context, mountPoint, parent);
    }

    @Override
    public final FsController newController(
            final FsManager context,
            final FsModel model,
            final Optional<? extends FsController> parent)
    throws ServiceConfigurationError {
        assert model.getParent().equals(parent.map(FsController::getModel));
        return driver(model.getMountPoint()).newController(context, model, parent);
    }

    private FsDriver driver(final FsMountPoint mountPoint) {
        final FsScheme scheme = mountPoint.getScheme();
        final FsDriver driver = get().get(scheme);
        if (null == driver) {
            throw new ServiceConfigurationError(scheme
                    + " (Unknown file system scheme! May be the class path doesn't contain the respective driver module or it isn't set up correctly?)");
        }
        return driver;
    }
}
