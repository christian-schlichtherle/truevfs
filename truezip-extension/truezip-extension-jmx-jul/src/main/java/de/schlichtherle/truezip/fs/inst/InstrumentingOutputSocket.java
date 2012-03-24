/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.cio.DecoratingOutputSocket;
import de.schlichtherle.truezip.cio.Entry;
import de.schlichtherle.truezip.cio.OutputSocket;
import javax.annotation.concurrent.Immutable;

/**
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class InstrumentingOutputSocket<E extends Entry>
extends DecoratingOutputSocket<E> {

    protected final InstrumentingDirector<?> director;

    protected InstrumentingOutputSocket(
            final OutputSocket<? extends E> delegate,
            final InstrumentingDirector<?> director) {
        super(delegate);
        if (null == (this.director = director))
            throw new NullPointerException();
    }
}