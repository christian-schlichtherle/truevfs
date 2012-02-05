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
 * An abstract decorator for an input shop.
 *
 * @param   <E> The type of the entries served by the decorated input shop.
 * @param   <I> The type of the decorated input shop.
 * @see     DecoratingOutputShop
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class DecoratingInputShop<E extends Entry, I extends InputShop<E>>
extends DecoratingEntryContainer<E, I>
implements InputShop<E> {

    @CreatesObligation
    protected DecoratingInputShop(final @WillCloseWhenClosed I delegate) {
        super(delegate);
    }

    @Override
    public InputSocket<? extends E> getInputSocket(String name) {
        return delegate.getInputSocket(name);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
