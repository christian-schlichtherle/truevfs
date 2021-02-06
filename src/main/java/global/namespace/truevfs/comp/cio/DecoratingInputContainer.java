/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

/**
 * An abstract decorator for an input container.
 *
 * @param <E> the type of the entries in the decorated input container.
 * @author Christian Schlichtherle
 * @see DecoratingOutputContainer
 */
public abstract class DecoratingInputContainer<E extends Entry>
        extends DecoratingContainer<E, InputContainer<E>> implements DelegatingInputContainer<E> {

    protected DecoratingInputContainer() {
    }

    protected DecoratingInputContainer(InputContainer<E> input) {
        super(input);
    }
}
