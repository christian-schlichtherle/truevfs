/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.TestConfig;
import de.truezip.kernel.ThrowControl;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.util.Iterator;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @param   <E> The type of the entries served to the decorated output service.
 * @see     ThrowingInputService
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public class ThrowingOutputService<E extends Entry>
extends DecoratingOutputService<E, OutputService<E>> {
    private final TestConfig config;
    private volatile @CheckForNull ThrowControl control;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public ThrowingOutputService(  final @WillCloseWhenClosed OutputService<E> service) {
        this(service, null);
    }

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public ThrowingOutputService(  final @WillCloseWhenClosed OutputService<E> service,
                                final @CheckForNull TestConfig config) {
        super(service);
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
    public int size() {
        checkUndeclaredExceptions();
        return delegate.size();
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
    public OutputSocket<E> getOutputSocket(E entry) {
        checkUndeclaredExceptions();
        return delegate.getOutputSocket(entry);
    }
}