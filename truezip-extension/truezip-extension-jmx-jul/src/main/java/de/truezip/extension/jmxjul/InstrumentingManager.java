/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul;

import de.truezip.kernel.*;
import java.util.Objects;
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
        this.director = Objects.requireNonNull(director);
    }

    @Override
    public FsController<?> controller(FsMountPoint mountPoint, FsCompositeDriver driver) {
        return director.instrument(manager.controller(mountPoint, director.instrument(driver, this)), this);
    }
}
