/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.cio.DecoratingOutputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @param  <D> the type of the instrumenting director.
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    InstrumentingOutputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public class InstrumentingOutputSocket<
        D extends InstrumentingDirector<D>,
        E extends Entry>
extends DecoratingOutputSocket<E> {
    protected final D director;

    public InstrumentingOutputSocket(
            final D director,
            final OutputSocket<? extends E> socket) {
        super(socket);
        this.director = Objects.requireNonNull(director);
    }
}
