/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.entry;

import de.schlichtherle.truezip.socket.InputSocket;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import javax.annotation.WillCloseWhenClosed;

/**
 * An abstract decorator for an input service.
 *
 * @param  <E> the type of the entries served by the decorated input service.
 * @param  <I> the type of the decorated input service.
 * @see    DecoratingOutputService
 * @author Christian Schlichtherle
 */
public abstract class DecoratingInputService<E extends Entry, I extends InputService<E>>
extends DecoratingEntryContainer<E, I>
implements InputService<E> {

    @CreatesObligation
    protected DecoratingInputService(final @WillCloseWhenClosed I delegate) {
        super(delegate);
    }

    @Override
    public InputSocket<? extends E> getInputSocket(String name) {
        return delegate.getInputSocket(name);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
