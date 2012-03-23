/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.entry;

import de.schlichtherle.truezip.entry.DecoratingEntryContainer;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.socket.OutputSocket;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import javax.annotation.WillCloseWhenClosed;

/**
 * An abstract decorator for an output service.
 *
 * @param  <E> the type of the entries served to the decorated output service.
 * @param  <O> the type of the decorated output service.
 * @see    DecoratingInputService
 * @author Christian Schlichtherle
 */
public abstract class DecoratingOutputService<E extends Entry, O extends OutputService<E>>
extends DecoratingEntryContainer<E, O>
implements OutputService<E> {

    @CreatesObligation
    protected DecoratingOutputService(final @WillCloseWhenClosed O delegate) {
        super(delegate);
    }

    @Override
    public OutputSocket<? extends E> getOutputSocket(E entry) {
        return delegate.getOutputSocket(entry);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
