/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.fs.FsCompositeDriver;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDecoratingManager;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsMountPoint;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class InstrumentingManager
extends FsDecoratingManager<FsManager> {

    protected final InstrumentingDirector director;

    public InstrumentingManager(final FsManager manager, final InstrumentingDirector director) {
        super(manager);
        this.director = director.check();
    }

    @Override
    public FsController<?> getController(FsMountPoint mountPoint, FsCompositeDriver driver) {
        return director.instrument(delegate.getController(mountPoint, director.instrument(driver, this)), this);
    }
}
