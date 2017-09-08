/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.cio;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.util.Iterator;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.cio.DecoratingOutputService;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.OutputService;
import net.java.truecommons.cio.OutputSocket;
import net.java.truevfs.kernel.spec.FsTestConfig;
import net.java.truevfs.kernel.spec.FsThrowManager;

/**
 * @param   <E> The type of the entries served to the decorated output service.
 * @see     ThrowingInputService
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public class ThrowingOutputService<E extends Entry>
extends DecoratingOutputService<E> {
    private final FsTestConfig config;
    private volatile @CheckForNull FsThrowManager control;

    public ThrowingOutputService(
            final @WillCloseWhenClosed OutputService<E> service) {
        this(service, null);
    }

    public ThrowingOutputService(
            final @WillCloseWhenClosed OutputService<E> service,
            final @CheckForNull FsTestConfig config) {
        super(service);
        this.config = null != config ? config : FsTestConfig.get();
    }

    private FsThrowManager getThrowControl() {
        final FsThrowManager control = this.control;
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
