/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.SafeKeyManager;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.IOException;
import java.net.URI;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This file system controller decorates another file system controller in
 * order to manage the keys required for accessing encrypted ZIP files.
 * 
 * @param  <D> the type of the ZIP driver.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class KeyManagerController<D extends ZipDriver>
extends FsDecoratingController<FsModel, FsController<?>> {

    protected final D driver;
    private volatile KeyManager<?> manager;

    /**
     * Constructs a new key manager controller.
     *
     * @param controller the non-{@code null} file system controller to
     *        decorate.
     * @param driver the ZIP driver.
     */
    protected KeyManagerController(
            final FsController<?> controller,
            final D driver) {
        super(controller);
        if (null == driver)
            throw new NullPointerException();
        this.driver = driver;
    }

    protected abstract Class<?> getKeyType();

    protected abstract Class<? extends IOException> getKeyExceptionType();

    private KeyManager<?> getKeyManager() {
        final KeyManager<?> manager = this.manager;
        return null != manager
                ? manager
                : (this.manager = driver.getKeyManagerProvider()
                    .get(getKeyType()));
    }

    @Override
    public void unlink(final FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        try {
            delegate.unlink(name, options);
        } catch (final FsFalsePositiveException ex) {
            // If the false positive exception is caused by a key exception,
            // then throw this cause instead in order to avoid delegating
            // this operation to the parent file system.
            // This prevents the application from inadvertently deleting an
            // encrypted ZIP file just because e.g. the user has cancelled key
            // prompting.
            final IOException cause = ex.getCause();
            if (null != cause
                    && getKeyExceptionType().isAssignableFrom(cause.getClass()))
                throw cause;
            throw ex;
        }
        if (name.isRoot())
            getKeyManager().removeKeyProvider(
                    driver.resourceUri(getModel(), name.toString()));
    }

    @Override
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        delegate.sync(options, handler);
        final KeyManager<?> manager = getKeyManager();
        final URI resource = driver.mountPointUri(getModel());
        final KeyProvider<?> provider;
        if (manager instanceof SafeKeyManager) {
            // Don't create a key provider if there wasn't one mapped already.
            provider = ((SafeKeyManager) manager).getMappedKeyProvider(resource);
        } else {
            // TODO: This might create a memory leak.
            // It's unlikely that a third party implements the KeyManager
            // interface and does NOT use extend the SafeKeyManager class,
            // though.
            provider = manager.getKeyProvider(resource);
        }
        if (null != provider)
            driver.getKeyProviderSyncStrategy().sync(provider);
    }
}
