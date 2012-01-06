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
import de.schlichtherle.truezip.fs.FsModel;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class InstrumentingCompositeDriver implements FsCompositeDriver {

    protected final FsCompositeDriver delegate;
    protected final InstrumentingDirector director;

    public InstrumentingCompositeDriver(final FsCompositeDriver driver, final InstrumentingDirector director) {
        if (null == driver)
            throw new NullPointerException();
        this.director = director.check();
        this.delegate = driver;
    }

    @Override
    public FsController<?> newController(FsModel model, FsController<?> parent) {
        return director.instrument(delegate.newController(director.instrument(model, this), parent), this);
    }
}
