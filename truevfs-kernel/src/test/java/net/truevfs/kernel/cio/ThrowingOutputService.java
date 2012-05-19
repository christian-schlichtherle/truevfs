/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import net.truevfs.kernel.TestConfig;
import net.truevfs.kernel.ThrowManager;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
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
    private volatile @CheckForNull ThrowManager control;

    public ThrowingOutputService(
            final @WillCloseWhenClosed OutputService<E> service) {
        this(service, null);
    }

    public ThrowingOutputService(
            final @WillCloseWhenClosed OutputService<E> service,
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
    public OutputSocket<E> output(E entry) {
        checkUndeclaredExceptions();
        return container.output(entry);
    }
}
