/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.inst;

import java.util.Objects;
import java.util.ServiceConfigurationError;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.FsCompositeDriver;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManagerWithControllerFactory;
import net.java.truevfs.kernel.spec.FsModel;

/**
 * @param  <M> the type of the mediator.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingMetaDriver<M extends Mediator<M>>
implements FsCompositeDriver {

    protected final M mediator;
    protected final FsCompositeDriver driver;

    public InstrumentingMetaDriver(
            final M mediator,
            final FsCompositeDriver driver) {
        this.mediator = Objects.requireNonNull(mediator);
        this.driver = Objects.requireNonNull(driver);
    }

    @Override
    public FsController newController(
            final FsManagerWithControllerFactory manager,
            final FsModel model,
            final @CheckForNull FsController parent)
    throws ServiceConfigurationError {
        assert null == parent
                    ? null == model.getParent()
                    : parent.getModel().equals(model.getParent());
        return mediator.instrument(this,
                driver.newController(
                    manager,
                    mediator.instrument(this, model),
                    parent));
    }

    @Override
    public String toString() {
        return String.format("%s[driver=%s]", getClass().getName(), driver);
    }
}
