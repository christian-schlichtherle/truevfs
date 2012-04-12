/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import javax.annotation.WillCloseWhenClosed;

/**
 * An abstract decorator for an input service.
 *
 * @param  <E> the type of the entries in the decorated input service.
 * @param  <I> the type of the decorated input service.
 * @see    DecoratingOutputService
 * @author Christian Schlichtherle
 */
public abstract class DecoratingInputService<   E extends Entry,
                                                I extends InputService<E>>
extends DecoratingContainer<E, I>
implements InputService<E> {

    protected DecoratingInputService() { }

    protected DecoratingInputService(@WillCloseWhenClosed I input) {
        super(input);
    }

    @Override
    public InputSocket<E> inputSocket(String name) {
        return container.inputSocket(name);
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        container.close();
    }
}
