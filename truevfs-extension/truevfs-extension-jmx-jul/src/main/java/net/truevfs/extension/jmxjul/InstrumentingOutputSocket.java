/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul;

import net.truevfs.kernel.cio.DecoratingOutputSocket;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.OutputSocket;
import java.util.Objects;
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
        this.director = Objects.requireNonNull(director);
    }
}
