/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
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
 * @param   <E> The type of the entries served to the decorated output shop.
 * @see     ThrowingInputShop
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public class ThrowingOutputShop<E extends Entry>
extends DecoratingOutputShop<E, OutputShop<E>> {
    private final TestConfig config;
    private volatile @CheckForNull ThrowControl control;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public ThrowingOutputShop(  final @WillCloseWhenClosed OutputShop<E> shop) {
        this(shop, null);
    }

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public ThrowingOutputShop(  final @WillCloseWhenClosed OutputShop<E> shop,
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
    public OutputSocket<? extends E> getOutputSocket(E entry) {
        checkUndeclaredExceptions();
        return delegate.getOutputSocket(entry);
    }
}