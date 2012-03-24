/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.entry.DecoratingOutputSocket;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.OutputSocket;
import javax.annotation.concurrent.Immutable;

/**
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