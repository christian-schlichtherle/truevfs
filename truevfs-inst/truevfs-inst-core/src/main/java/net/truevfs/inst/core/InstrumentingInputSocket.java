/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.inst.core;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.cio.DecoratingInputSocket;
import net.truevfs.kernel.spec.cio.Entry;
import net.truevfs.kernel.spec.cio.InputSocket;

/**
 * @param  <E> the type of the {@linkplain #localTarget() local target}.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class InstrumentingInputSocket<E extends Entry>
extends DecoratingInputSocket<E> {

    protected final InstrumentingDirector<?> director;

    protected InstrumentingInputSocket(
            final InstrumentingDirector<?> director,
            final InputSocket<? extends E> socket) {
        super(socket);
        this.director = Objects.requireNonNull(director);
    }
}
