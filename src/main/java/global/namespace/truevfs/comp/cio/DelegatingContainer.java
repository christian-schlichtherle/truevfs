/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

/**
 * Forwards all calls to another container.
 *
 * @param <E> the type of the entries in the delegate container.
 * @author Christian Schlichtherle
 */
public interface DelegatingContainer<E extends Entry> extends Container<E> {

    /**
     * Returns the delegate container.
     */
    Container<E> getContainer();

    @Override
    default Collection<E> entries() throws IOException {
        return getContainer().entries();
    }

    @Override
    default Optional<E> entry(String name) throws IOException {
        return getContainer().entry(name);
    }

    @Override
    default void close() throws IOException {
        getContainer().close();
    }
}
