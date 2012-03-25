/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul;

import de.truezip.kernel.fs.FsCompositeDriver;
import de.truezip.kernel.fs.FsController;
import de.truezip.kernel.fs.FsDecoratingManager;
import de.truezip.kernel.fs.FsManager;
import de.truezip.kernel.fs.addr.FsMountPoint;
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