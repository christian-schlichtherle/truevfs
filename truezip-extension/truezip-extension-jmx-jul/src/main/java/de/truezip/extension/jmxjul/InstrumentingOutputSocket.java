/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul;

import de.truezip.kernel.cio.DecoratingOutputSocket;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.OutputSocket;
import javax.annotation.concurrent.Immutable;

/**
 * @param  <E> the type of the {@linkplain #localTarget() local target}.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class InstrumentingOutputSocket<E extends Entry>
extends DecoratingOutputSocket<E> {

    protected final InstrumentingDirector<?> director;

    protected InstrumentingOutputSocket(
            final OutputSocket<? extends E> socket,
            final InstrumentingDirector<?> director) {
        super(socket);
        if (null == (this.director = director))
            throw new NullPointerException();
    }
}
