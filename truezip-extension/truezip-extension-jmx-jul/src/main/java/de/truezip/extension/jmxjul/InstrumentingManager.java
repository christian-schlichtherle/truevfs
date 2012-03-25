/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul;

import de.schlichtherle.truezip.fs.FsCompositeDriver;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDecoratingManager;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.addr.FsMountPoint;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingManager extends FsDecoratingManager<FsManager> {

    protected final InstrumentingDirector<?> director;

    public InstrumentingManager(final FsManager manager,
                                final InstrumentingDirector<?> director) {
        super(manager);
        if (null == (this.director = director))
            throw new NullPointerException();
    }

    @Override
    public FsController<?> getController(FsMountPoint mountPoint, FsCompositeDriver driver) {
        return director.instrument(delegate.getController(mountPoint, director.instrument(driver, this)), this);
    }
}