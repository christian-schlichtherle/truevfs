/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.fs.*;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
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
