/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.FsCompositeDriver;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsModel;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingCompositeDriver implements FsCompositeDriver {

    protected final InstrumentingDirector<?> director;
    protected final FsCompositeDriver driver;

    public InstrumentingCompositeDriver(
            final InstrumentingDirector<?> director,
            final FsCompositeDriver driver) {
        this.director = Objects.requireNonNull(director);
        this.driver = Objects.requireNonNull(driver);
    }

    @Override
    public FsController<? extends FsModel> newController(
            final FsManager manager,
            final FsModel model,
            final @CheckForNull FsController<? extends FsModel> parent) {
        assert null == parent
                    ? null == model.getParent()
                    : parent.getModel().equals(model.getParent());
        return director.instrument(driver.newController(manager, director.instrument(model, this), parent), this);
    }

    @Override
    public String toString() {
        return String.format("%s[driver=%s]", getClass().getName(), driver);
    }
}
