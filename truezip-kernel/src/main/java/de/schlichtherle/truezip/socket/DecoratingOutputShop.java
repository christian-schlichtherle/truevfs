/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.DecoratingEntryContainer;
import de.schlichtherle.truezip.entry.Entry;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import javax.annotation.WillCloseWhenClosed;

/**
 * An abstract decorator for an output shop.
 *
 * @param  <E> the type of the entries served to the decorated output shop.
 * @param  <O> the type of the decorated output shop.
 * @see    DecoratingInputShop
 * @author Christian Schlichtherle
 */
public abstract class DecoratingOutputShop<E extends Entry, O extends OutputShop<E>>
extends DecoratingEntryContainer<E, O>
implements OutputShop<E> {

    @CreatesObligation
    protected DecoratingOutputShop(final @WillCloseWhenClosed O delegate) {
        super(delegate);
    }

    @Override
    public OutputSocket<? extends E> getOutputSocket(E entry) {
        return delegate.getOutputSocket(entry);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
