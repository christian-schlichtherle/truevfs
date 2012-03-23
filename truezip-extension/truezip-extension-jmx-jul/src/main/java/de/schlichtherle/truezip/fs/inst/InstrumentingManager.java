/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.fs.path.FsMountPoint;
import de.schlichtherle.truezip.fs.*;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
public class InstrumentingManager
extends FsDecoratingManager<FsManager> {

    protected final InstrumentingDirector director;

    public InstrumentingManager(final FsManager manager, final InstrumentingDirector director) {
        super(manager);
        if (null == director)
            throw new NullPointerException();
        this.director = director;
    }

    @Override
    public FsController<?> getController(FsMountPoint mountPoint, FsCompositeDriver driver) {
        return director.instrument(delegate.getController(mountPoint, director.instrument(driver, this)), this);
    }
}