/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

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
public abstract class DecoratingOutputService<  E extends Entry,
                                                O extends OutputService<E>>
extends DecoratingContainer<E, O>
implements OutputService<E> {

    @CreatesObligation
    protected DecoratingOutputService(final @WillCloseWhenClosed O service) {
        super(service);
    }

    @Override
    public OutputSocket<E> getOutputSocket(E entry) {
        return container.getOutputSocket(entry);
    }

    @Override
    public void close() throws IOException {
        container.close();
    }
}
