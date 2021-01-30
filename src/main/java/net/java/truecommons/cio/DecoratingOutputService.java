/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.cio;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import javax.annotation.WillCloseWhenClosed;

/**
 * An abstract decorator for an output service.
 *
 * @param  <E> the type of the entries in the decorated output service.
 * @see    DecoratingInputService
 * @author Christian Schlichtherle
 */
public abstract class DecoratingOutputService<E extends Entry>
extends DecoratingContainer<E, OutputService<E>> implements OutputService<E> {

    protected DecoratingOutputService() { }

    protected DecoratingOutputService(@WillCloseWhenClosed OutputService<E> output) {
        super(output);
    }

    @Override
    public OutputSocket<E> output(E entry) { return container.output(entry); }

    @Override
    @DischargesObligation
    public void close() throws IOException { container.close(); }
}
