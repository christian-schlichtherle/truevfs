/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul;

import net.truevfs.kernel.FsCompositeDriver;
import net.truevfs.kernel.FsController;
import net.truevfs.kernel.FsManager;
import net.truevfs.kernel.FsModel;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingCompositeDriver implements FsCompositeDriver {

    protected final FsCompositeDriver driver;
    protected final InstrumentingDirector<?> director;

    public InstrumentingCompositeDriver(final FsCompositeDriver driver,
                                        final InstrumentingDirector<?> director) {
        this.driver = Objects.requireNonNull(driver);
        this.director = Objects.requireNonNull(director);
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
