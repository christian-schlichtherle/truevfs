/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.cio;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.util.Iterator;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.kernel.spec.TestConfig;
import net.truevfs.kernel.spec.ThrowManager;

/**
 * @param   <E> The type of the entries served by the decorated input service.
 * @see     ThrowingOutputService
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public class ThrowingInputService<E extends Entry>
extends DecoratingInputService<E, InputService<E>> {
    private final TestConfig config;
    private volatile @CheckForNull ThrowManager control;

    public ThrowingInputService(
            final @WillCloseWhenClosed InputService<E> service) {
        this(service, null);
    }

    public ThrowingInputService(
            final @WillCloseWhenClosed InputService<E> service,
            final @CheckForNull TestConfig config) {
        super(service);
        this.config = null != config ? config : TestConfig.get();
    }

    private ThrowManager getThrowControl() {
        final ThrowManager control = this.control;
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
        return container.size();
    }

    @Override
    public Iterator<E> iterator() {
        checkUndeclaredExceptions();
        return container.iterator();
    }

    @Override
    public E entry(String name) {
        checkUndeclaredExceptions();
        return container.entry(name);
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        checkAllExceptions();
        container.close();
    }

    @Override
    public InputSocket<E> input(String name) {
        checkUndeclaredExceptions();
        return container.input(name);
    }
}
