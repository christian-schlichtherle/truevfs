/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul;

import de.truezip.kernel.FsCompositeDriver;
import de.truezip.kernel.FsController;
import de.truezip.kernel.FsDecoratingManager;
import de.truezip.kernel.FsManager;
import de.truezip.kernel.addr.FsMountPoint;
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
        return director.instrument(manager.getController(mountPoint, director.instrument(driver, this)), this);
    }
}