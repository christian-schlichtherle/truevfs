/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import javax.annotation.WillCloseWhenClosed;

/**
 * An abstract decorator for an output service.
 *
 * @param  <E> the type of the entries in the decorated output service.
 * @param  <O> the type of the decorated output service.
 * @see    DecoratingInputService
 * @author Christian Schlichtherle
 */
public abstract class DecoratingOutputService<  E extends Entry,
                                                O extends OutputService<E>>
extends DecoratingContainer<E, O>
implements OutputService<E> {

    protected DecoratingOutputService() { }

    protected DecoratingOutputService(@WillCloseWhenClosed O output) {
        super(output);
    }

    @Override
    public OutputSocket<E> output(E entry) {
        return container.output(entry);
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        container.close();
    }
}
