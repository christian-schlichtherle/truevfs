/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import static de.truezip.kernel.FsEntryName.ROOT;
import de.truezip.kernel.*;
import de.truezip.kernel.cio.Entry;
import static de.truezip.kernel.cio.Entry.Type.SPECIAL;
import de.truezip.kernel.util.BitField;
import de.truezip.key.KeyManager;
import java.io.IOException;
import java.util.ServiceConfigurationError;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This file system controller decorates another file system controller in
 * order to manage the keys required for accessing encrypted ZIP files.
 * 
 * @param  <M> the type of the file system model.
 * @param  <D> the type of the ZIP driver.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class KeyController<
        M extends FsModel,
        D extends ZipDriver>
extends FsDecoratingController<M, FsController<? extends M>> {

    private static final String ROOT_PATH = ROOT.getPath();

    protected final D driver;
    private volatile KeyManager<?> manager;

    /**
     * Constructs a new key manager controller.
     *
     * @param controller the non-{@code null} file system controller to
     *        decorate.
     * @param driver the ZIP driver.
     */
    protected KeyController(
            final FsController<? extends M> controller,
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
                : (this.manager = driver.getKeyManagerProvider().getKeyManager(getKeyType()));
    }

    @Override
    public final FsEntry entry(final FsEntryName name)
    throws IOException {
        try {
            return controller.entry(name);
        } catch (final Throwable ex) {
            if (!name.isRoot() || null == findKeyException(ex))
                throw ex;
            Entry entry = getParent().entry(
                    getModel()
                        .getMountPoint()
                        .getPath()
                        .resolve(name)
                        .getEntryName());
            // We're not holding any locks, so it's possible that someone else
            // has concurrently modified the parent file system.
            if (null == entry)
                return null;
            // The entry is inaccessible for some reason.
            // This may be because the cipher key is not available.
            // Now mask the entry as a special file.
            while (entry instanceof FsCovariantEntry<?>)
                entry = ((FsCovariantEntry<?>) entry).getEntry();
            final FsCovariantEntry<FsArchiveEntry>
                    special = new FsCovariantEntry<>(ROOT_PATH);
            special.putEntry(SPECIAL, driver.newEntry(ROOT_PATH, SPECIAL, entry));
            return special;
        }
    }

    @Override
    public final void unlink(   final FsEntryName name,
                                final BitField<FsAccessOption> options)
    throws IOException {
        try {
            controller.unlink(name, options);
        } catch (final Throwable ex) {
            // If the exception is caused by a key exception, then throw this
            // cause instead in order to avoid treating the target archive file
            // like a false positive and routing this operation to the parent
            // file system.
            // This prevents the application from inadvertently deleting an
            // encrypted ZIP file just because the user has cancelled key
            // prompting.
            final IOException keyEx = findKeyException(ex);
            if (null != keyEx)
                throw keyEx;
            throw ex;
        }
        if (name.isRoot()) {
            try {
                getKeyManager().delete(
                        driver.resourceUri(getModel(), name.toString()));
            } catch (final ServiceConfigurationError ignore) {
                // The operation succeeded without a key manager.
                // This can only mean that the target archive file doesn't
                // require any keys, so we can and should ignore this exception.
            }
        }
    }

    private @CheckForNull IOException findKeyException(Throwable ex) {
        final Class<? extends IOException> clazz = getKeyExceptionType();
        do {
            if (clazz.isInstance(ex))
                return clazz.cast(ex);
        } while (null != (ex = ex.getCause()));
        return null;
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
        try {
            controller.sync(options);
        } catch (final FsSyncWarningException ex) {
            builder.warn(ex);
        }
        try {
            getKeyManager().unlock(driver.mountPointUri(getModel()));
        } catch (final ServiceConfigurationError ignore) {
            // The operation succeeded without a key manager.
            // This can only mean that the target archive file doesn't
            // require any keys, so we can and should ignore this exception.
        }
        builder.check();
    }
}