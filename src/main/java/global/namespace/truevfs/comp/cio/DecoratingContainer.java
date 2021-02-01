/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import java.util.Objects;

/**
 * An abstract decorator for a container.
 *
 * @param <E> the type of the entries in the decorated container.
 * @param <C> the type of the decorated entry container.
 * @author Christian Schlichtherle
 */
public abstract class DecoratingContainer<E extends Entry, C extends Container<E>> implements DelegatingContainer<E> {

    /**
     * The decorated container.
     */
    protected C container;

    protected DecoratingContainer() {
    }

    protected DecoratingContainer(final C container) {
        this.container = Objects.requireNonNull(container);
    }

    public C getContainer() {
        return container;
    }
}
