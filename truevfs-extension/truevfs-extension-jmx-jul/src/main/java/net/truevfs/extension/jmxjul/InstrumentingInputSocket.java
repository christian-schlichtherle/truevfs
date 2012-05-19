/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul;

import net.truevfs.kernel.cio.DecoratingInputSocket;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.InputSocket;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/**
 * @param  <E> the type of the {@linkplain #localTarget() local target}.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class InstrumentingInputSocket<E extends Entry>
extends DecoratingInputSocket<E> {

    protected final InstrumentingDirector<?> director;

    protected InstrumentingInputSocket(
            final InputSocket<? extends E> socket,
            final InstrumentingDirector<?> director) {
        super(socket);
        this.director = Objects.requireNonNull(director);
    }
}