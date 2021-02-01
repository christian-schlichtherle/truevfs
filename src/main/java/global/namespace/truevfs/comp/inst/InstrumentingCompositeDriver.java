/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.inst;

import global.namespace.truevfs.kernel.api.*;

import java.util.Optional;
import java.util.ServiceConfigurationError;

import static java.util.Objects.requireNonNull;

/**
 * @param <M> the type of the mediator.
 * @author Christian Schlichtherle
 */
public class InstrumentingCompositeDriver<M extends Mediator<M>>
        implements FsCompositeDriver {

    protected final M mediator;
    protected final FsCompositeDriver driver;

    public InstrumentingCompositeDriver(
            final M mediator,
            final FsCompositeDriver driver) {
        this.mediator = requireNonNull(mediator);
        this.driver = requireNonNull(driver);
    }

    @Override
    public final FsModel newModel(
            FsManager context,
            FsMountPoint mountPoint,
            Optional<? extends FsModel> parent) {
        assert mountPoint.getParent().equals(parent.map(FsModel::getMountPoint));
        return mediator.instrument(this, driver.newModel(context, mountPoint, parent));
    }

    @Override
    public FsController newController(
            final FsManager context,
            final FsModel model,
            final Optional< ? extends FsController> parent)
            throws ServiceConfigurationError {
        assert parent.map(FsController::getModel).equals(model.getParent());
        return mediator.instrument(this, driver.newController(context, model, parent));
    }

    @Override
    public String toString() {
        return String.format("%s[driver=%s]", getClass().getName(), driver);
    }
}
