/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.core;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import static net.truevfs.kernel.spec.FsEntryName.ROOT;
import net.truevfs.kernel.spec.*;
import net.truevfs.kernel.spec.cio.Entry;
import net.truevfs.kernel.spec.cio.Entry.Access;
import static net.truevfs.kernel.spec.cio.Entry.Type.SPECIAL;
import net.truevfs.kernel.spec.util.BitField;
import net.truevfs.kernel.spec.util.ControlFlowException;
import net.truevfs.keymgr.spec.KeyManager;
import net.truevfs.keymgr.spec.KeyManagerContainer;

/**
 * This file system controller decorates another file system controller in
 * order to manage the keys required for accessing encrypted ZIP files.
 * 
 * @param  <M> the type of the file system model.
 * @param  <D> the type of the ZIP driver.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class KeyController<
        M extends FsModel,
        D extends AbstractZipDriver>
extends FsDecoratingController<M, FsController<? extends M>> {

    private static final String ROOT_PATH = ROOT.getPath();

    protected final D driver;
    private volatile KeyManagerContainer provider;

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
        this.driver = Objects.requireNonNull(driver);
    }

    protected abstract Class<?> getKeyType();

    protected abstract Class<? extends IOException> getKeyExceptionType();

    private @CheckForNull KeyManager<?> getKeyManager() {
        final KeyManagerContainer provider = this.provider;
        return (null != provider
                    ? provider
                    : (this.provider = driver.getKeyManagerProvider()))
                .keyManager(getKeyType());
    }

    private @CheckForNull IOException findKeyException(Throwable ex) {
        final Class<? extends IOException> clazz = getKeyExceptionType();
        do {
            if (clazz.isInstance(ex)) return clazz.cast(ex);
        } while (null != (ex = ex.getCause()));
        return null;
    }

    @Override
    public final FsEntry stat(
            final BitField<FsAccessOption> options, final FsEntryName name)
    throws IOException {
        try {
            return controller.stat(options, name);
        } catch (final ControlFlowException ex) {
            if (!name.isRoot() || null == findKeyException(ex)) throw ex;
            Entry entry = getParent().stat(
                    options, getModel()
                                 .getMountPoint()
                                 .getPath()
                                 .resolve(name)
                                 .getEntryName());
            // We're not holding any locks, so it's possible that someone else
            // has concurrently modified the parent file system.
            if (null == entry) return null;
            // The entry is inaccessible for some reason.
            // This may be because the cipher key is not available.
            // Now mask the entry as a special file.
            if (entry instanceof FsCovariantEntry<?>)
                entry = ((FsCovariantEntry<?>) entry).getEntry();
            final FsCovariantEntry<FsArchiveEntry>
                    special = new FsCovariantEntry<>(ROOT_PATH);
            special.put(SPECIAL, driver.newEntry(ROOT_PATH, SPECIAL, entry));
            return special;
        }
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options, final FsEntryName name, final BitField<Access> types)
    throws IOException {
        try {
            controller.checkAccess(options, name, types);
        } catch (final ControlFlowException ex) {
            if (!name.isRoot() || null == findKeyException(ex)) throw ex;
            getParent().checkAccess(
                    options, getModel()
                                 .getMountPoint()
                                 .getPath()
                                 .resolve(name)
                                 .getEntryName(),
                    types);
        }
    }

    @Override
    public final void unlink(   final BitField<FsAccessOption> options, final FsEntryName name)
    throws IOException {
        try {
            controller.unlink(options, name);
        } catch (final ControlFlowException ex) {
            // If the exception is caused by a key exception, then throw this
            // cause instead in order to avoid treating the target archive file
            // like a false positive and routing this operation to the parent
            // file system.
            // This prevents the application from inadvertently deleting an
            // encrypted ZIP file just because the user cancelled key prompting.
            final IOException keyEx = findKeyException(ex);
            if (null == keyEx) throw ex;
            throw keyEx;
        }
        final FsModel model = getModel();
        final URI mpu = driver.mountPointUri(model);
        final URI fsu = driver.fileSystemUri(model, name.toString());
        if (!fsu.equals(mpu) || name.isRoot()) {
            final KeyManager<?> keyManager = getKeyManager();
            if (null != keyManager)
                keyManager.delete(fsu);
        }
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
        final KeyManager<?> keyManager = getKeyManager();
        if (null != keyManager)
            keyManager.unlock(driver.mountPointUri(getModel()));
        builder.check();
    }
}