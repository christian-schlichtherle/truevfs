/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
public abstract class InstrumentingInputSocket<E extends Entry>
extends DecoratingInputSocket<E> {

    protected final InstrumentingDirector director;

    protected InstrumentingInputSocket(
            final InputSocket<? extends E> delegate,
            final InstrumentingDirector director) {
        super(delegate);
        if (null == director)
            throw new NullPointerException();
        this.director = director;
    }
}