/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.fs.FsCompositeDriver;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsModel;
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
    public FsController<?> newController(   final FsModel model,
                                            final FsController<?> parent) {
        return director.instrument(delegate.newController(director.instrument(model, this), parent), this);
    }
}