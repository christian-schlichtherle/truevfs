/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.inst;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.FsCompositeDriver;
import net.truevfs.kernel.spec.FsController;
import net.truevfs.kernel.spec.FsManager;
import net.truevfs.kernel.spec.FsModel;

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
    public FsController<?> newController(
            final FsManager manager,
            final FsModel model,
            final @CheckForNull FsController<?> parent) {
        assert null == parent
                    ? null == model.getParent()
                    : parent.getModel().equals(model.getParent());
        return director.instrument(driver.newController(manager, director.instrument(model, this), parent), this);
    }
}
