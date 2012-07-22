/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.comp.inst;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.cio.DecoratingOutputSocket;
import net.truevfs.kernel.spec.cio.Entry;
import net.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @param  <E> the type of the {@linkplain #localTarget() local target}.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class InstrumentingOutputSocket<E extends Entry>
extends DecoratingOutputSocket<E> {

    protected final InstrumentingDirector<?> director;

    protected InstrumentingOutputSocket(
            final InstrumentingDirector<?> director,
            final OutputSocket<? extends E> socket) {
        super(socket);
        this.director = Objects.requireNonNull(director);
    }
}
