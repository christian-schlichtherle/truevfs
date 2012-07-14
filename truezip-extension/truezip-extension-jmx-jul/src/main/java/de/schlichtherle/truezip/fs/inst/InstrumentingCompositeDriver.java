/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.fs.FsCompositeDriver;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsModel;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
public class InstrumentingCompositeDriver implements FsCompositeDriver {

    protected final FsCompositeDriver delegate;
    protected final InstrumentingDirector director;

    public InstrumentingCompositeDriver(final FsCompositeDriver driver,
                                        final InstrumentingDirector director) {
        if (null == driver || null == director)
            throw new NullPointerException();
        this.director = director;
        this.delegate = driver;
    }

    @Override
    public FsController<? extends FsModel> newController(
            final FsManager manager,
            final FsModel model,
            final @CheckForNull FsController<? extends FsModel> parent) {
        assert null == parent
                    ? null == model.getParent()
                    : parent.getModel().equals(model.getParent());
        return director.instrument(delegate.newController(manager, director.instrument(model, this), parent), this);
    }

    @Override
    public String toString() {
        return String.format("%s[delegate=%s]", getClass().getName(), delegate);
    }
}
