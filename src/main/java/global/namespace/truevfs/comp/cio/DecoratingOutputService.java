/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import java.io.IOException;

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

    protected DecoratingOutputService(OutputService<E> output) {
        super(output);
    }

    @Override
    public OutputSocket<E> output(E entry) { return container.output(entry); }

    @Override
    public void close() throws IOException { container.close(); }
}
