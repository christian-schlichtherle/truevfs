/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul;

import de.schlichtherle.truezip.cio.DecoratingInputSocket;
import de.schlichtherle.truezip.cio.Entry;
import de.schlichtherle.truezip.cio.InputSocket;
import javax.annotation.concurrent.Immutable;

/**
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class InstrumentingInputSocket<E extends Entry>
extends DecoratingInputSocket<E> {

    protected final InstrumentingDirector<?> director;

    protected InstrumentingInputSocket(
            final InputSocket<? extends E> delegate,
            final InstrumentingDirector<?> director) {
        super(delegate);
        if (null == (this.director = director))
            throw new NullPointerException();
    }
}