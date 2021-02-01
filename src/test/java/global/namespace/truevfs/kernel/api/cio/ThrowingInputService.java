/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api.cio;

import global.namespace.truevfs.comp.cio.DecoratingInputService;
import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.InputService;
import global.namespace.truevfs.comp.cio.InputSocket;
import global.namespace.truevfs.kernel.api.FsTestConfig;
import global.namespace.truevfs.kernel.api.FsThrowManager;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.Iterator;

/**
 * @param   <E> The type of the entries served by the decorated input service.
 * @see     ThrowingOutputService
 * @author  Christian Schlichtherle
 */
public class ThrowingInputService<E extends Entry>
extends DecoratingInputService<E> {
    private final FsTestConfig config;
    private volatile @CheckForNull FsThrowManager control;

    public ThrowingInputService(InputService<E> service) {
        this(service, null);
    }

    public ThrowingInputService(
            final InputService<E> service,
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
