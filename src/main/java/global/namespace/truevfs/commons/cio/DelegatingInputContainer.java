/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.cio;

import java.io.IOException;

/**
 * Forwards all calls to another input container.
 *
 * @param <E> the type of the entries in the delegate input container.
 * @author Christian Schlichtherle
 * @see DelegatingOutputContainer
 */
public interface DelegatingInputContainer<E extends Entry> extends DelegatingContainer<E>, InputContainer<E> {

    /**
     * Returns the delegate input container.
     */
    @Override
    InputContainer<E> getContainer();

    @Override
    default InputSocket<E> input(String name) {
        return getContainer().input(name);
    }

    @Override
    default void close() throws IOException {
        getContainer().close();
    }
}
