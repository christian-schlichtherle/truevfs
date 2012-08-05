/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.util.Objects;
import java.util.ServiceConfigurationError;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.FsCompositeDriver;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsModel;

/**
 * @param  <D> the type of the director.
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingCompositeDriver<D extends Director<D>>
implements FsCompositeDriver {
    protected final D director;
    protected final FsCompositeDriver driver;

    public InstrumentingCompositeDriver(
            final D director,
            final FsCompositeDriver driver) {
        this.director = Objects.requireNonNull(director);
        this.driver = Objects.requireNonNull(driver);
    }

    @Override
    public FsController newController(
            final FsManager manager,
            final FsModel model,
            final @CheckForNull FsController parent)
    throws ServiceConfigurationError {
        assert null == parent
                    ? null == model.getParent()
                    : parent.getModel().equals(model.getParent());
        return director.instrument(this,
                driver.newController(
                    manager,
                    director.instrument(this, model),
                    parent));
    }

    @Override
    public String toString() {
        return String.format("%s[driver=%s]", getClass().getName(), driver);
    }
}
