/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.cio;

import java.io.Closeable;

/**
 * A service for reading entries from this container.
 *
 * @param  <E> the type of the entries in this container.
 * @see    OutputContainer
 * @author Christian Schlichtherle
 */
public interface InputContainer<E extends Entry> extends Closeable, Container<E> {

    /**
     * Returns an input socket for reading from the entry with the given name.
     *
     * @param  name an {@link Entry#getName() entry name}.
     * @return An input socket for reading from the entry with the given name.
     */
    InputSocket<E> input(String name);
}
