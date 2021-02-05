/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.cio;

/**
 * An abstract decorator for an output container.
 *
 * @param <E> the type of the entries in the decorated output container.
 * @author Christian Schlichtherle
 * @see DecoratingInputContainer
 */
public abstract class DecoratingOutputContainer<E extends Entry>
        extends DecoratingContainer<E, OutputContainer<E>> implements DelegatingOutputContainer<E> {

    protected DecoratingOutputContainer() {
    }

    protected DecoratingOutputContainer(OutputContainer<E> output) {
        super(output);
    }
}
