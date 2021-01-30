/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.cio;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import javax.annotation.WillCloseWhenClosed;

/**
 * An abstract decorator for an input service.
 *
 * @param  <E> the type of the entries in the decorated input service.
 * @see    DecoratingOutputService
 * @author Christian Schlichtherle
 */
public abstract class DecoratingInputService<E extends Entry>
extends DecoratingContainer<E, InputService<E>> implements InputService<E> {

    protected DecoratingInputService() { }

    protected DecoratingInputService(@WillCloseWhenClosed InputService<E> input) {
        super(input);
    }

    @Override
    public InputSocket<E> input(String name) { return container.input(name); }

    @Override
    @DischargesObligation
    public void close() throws IOException { container.close(); }
}
