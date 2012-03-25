/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul;

import de.truezip.kernel.fs.FsDecoratingModel;
import de.truezip.kernel.fs.FsModel;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class InstrumentingModel extends FsDecoratingModel<FsModel> {

    protected final InstrumentingDirector<?> director;

    @SuppressWarnings("LeakingThisInConstructor")
    protected InstrumentingModel(   final FsModel model,
                                    final InstrumentingDirector<?> director) {
        super(model);
        if (null == (this.director = director))
            throw new NullPointerException();
    }
}