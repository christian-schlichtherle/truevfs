/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.test.TestConfig;
import de.schlichtherle.truezip.test.ThrowControl;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.util.Iterator;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @param   <E> The type of the entries served by the decorated input shop.
 * @see     ThrowingOutputShop
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ThrowingInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {
    private final TestConfig config;
    private volatile @CheckForNull ThrowControl control;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public ThrowingInputShop(   final @WillCloseWhenClosed InputShop<E> shop) {
        this(shop, null);
    }

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public ThrowingInputShop(   final @WillCloseWhenClosed InputShop<E> shop,
                                final @CheckForNull TestConfig config) {
        super(shop);
        this.config = null != config ? config : TestConfig.get();
    }

    private ThrowControl getThrowControl() {
        final ThrowControl control = this.control;
        return null != control ? control : (this.control = config.getThrowControl());
    }

    private void checkAllExceptions() throws IOException {
        getThrowControl().check(this, IOException.class);
        checkUndeclaredExceptions();
    }

    private void checkUndeclaredExceptions() {
        getThrowControl().check(this, RuntimeException.class);
        getThrowControl().check(this, Error.class);
    }

    @Override
    public int getSize() {
        checkUndeclaredExceptions();
        return delegate.getSize();
    }

    @Override
    public Iterator<E> iterator() {
        checkUndeclaredExceptions();
        return delegate.iterator();
    }

    @Override
    public E getEntry(String name) {
        checkUndeclaredExceptions();
        return delegate.getEntry(name);
    }

    @Override
    public void close() throws IOException {
        checkAllExceptions();
        delegate.close();
    }

    @Override
    public InputSocket<? extends E> getInputSocket(String name) {
        checkUndeclaredExceptions();
        return delegate.getInputSocket(name);
    }
}
