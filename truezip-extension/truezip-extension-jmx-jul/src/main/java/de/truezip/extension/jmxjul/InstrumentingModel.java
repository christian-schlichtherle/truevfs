/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul;

import de.truezip.kernel.FsDecoratingModel;
import de.truezip.kernel.FsModel;
import java.util.Objects;
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
        this.director = Objects.requireNonNull(director);
    }
}
