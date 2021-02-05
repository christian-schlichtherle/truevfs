/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.cio;

import java.io.IOException;

/**
 * Forwards all calls to another input container.
 *
 * @param <E> the type of the entries in the delegate output container.
 * @author Christian Schlichtherle
 * @see DelegatingInputContainer
 */
public interface DelegatingOutputContainer<E extends Entry> extends DelegatingContainer<E>, OutputContainer<E> {

    /**
     * Returns the delegate output container.
     */
    @Override
    OutputContainer<E> getContainer();

    @Override
    default OutputSocket<E> output(E entry) {
        return getContainer().output(entry);
    }

    @Override
    default void close() throws IOException {
        getContainer().close();
    }
}
