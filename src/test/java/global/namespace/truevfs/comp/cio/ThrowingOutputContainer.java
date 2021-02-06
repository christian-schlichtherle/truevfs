/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import global.namespace.truevfs.kernel.api.FsTestConfig;
import global.namespace.truevfs.kernel.api.FsThrowManager;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

/**
 * @param <E> The type of the entries served to the decorated output service.
 * @author Christian Schlichtherle
 * @see ThrowingInputContainer
 */
public class ThrowingOutputContainer<E extends Entry> extends DecoratingOutputContainer<E> {

    private final FsTestConfig config;
    private volatile @CheckForNull
    FsThrowManager control;

    public ThrowingOutputContainer(OutputContainer<E> service) {
        this(service, null);
    }

    public ThrowingOutputContainer(
            final OutputContainer<E> service,
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
    public Collection<E> entries() throws IOException {
        checkUndeclaredExceptions();
        return getContainer().entries();
    }

    @Override
    public Optional<E> entry(final String name) throws IOException {
        checkUndeclaredExceptions();
        return getContainer().entry(name);
    }

    @Override
    public void close() throws IOException {
        checkAllExceptions();
        getContainer().close();
    }

    @Override
    public OutputSocket<E> output(final E entry) {
        checkUndeclaredExceptions();
        return getContainer().output(entry);
    }
}
