/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * @param   <E> The type of the entries served to the decorated output shop.
 * @param   <O> The type of the decorated output shop.
 * @see     DecoratingInputShop
 * @author  Christian Schlichtherle
 * @version $Id$
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
