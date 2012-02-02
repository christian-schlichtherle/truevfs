/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.fs.FsDecoratingModel;
import de.schlichtherle.truezip.fs.FsModel;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public abstract class InstrumentingModel extends FsDecoratingModel<FsModel> {

    protected final InstrumentingDirector director;

    @SuppressWarnings("LeakingThisInConstructor")
    protected InstrumentingModel(FsModel model, InstrumentingDirector director) {
        super(model);
        if (null == director)
            throw new NullPointerException();
        this.director = director;
    }
}
