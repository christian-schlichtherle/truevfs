/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.Entry.Access;
import static net.java.truecommons.cio.Entry.Type.SPECIAL;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.ControlFlowException;
import net.java.truevfs.kernel.spec.*;
import static net.java.truevfs.kernel.spec.FsNodeName.ROOT;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.KeyManagerMap;

/**
 * This file system controller decorates another file system controller in
 * order to manage the keys required for accessing encrypted ZIP files.
 *
 * @param  <D> the type of the ZIP driver.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class AbstractKeyController<D extends AbstractZipDriver<?>>
extends FsDecoratingController {

    private static final String ROOT_PATH = ROOT.getPath();

    protected final D driver;
    private volatile KeyManagerMap container;

    /**
     * Constructs a new key manager controller.
     *
     * @param controller the non-{@code null} file system controller to
     *        decorate.
     * @param driver the ZIP driver.
     */
    protected AbstractKeyController(
            final FsController controller,
            final D driver) {
        super(controller);
        this.driver = Objects.requireNonNull(driver);
    }

    protected abstract Class<?> getKeyType();

    protected abstract Class<? extends IOException> getKeyExceptionType();

    private KeyManager<?> getKeyManager() {
        final KeyManagerMap c = this.container;
        return (null != c ? c : (this.container = driver.getKeyManagerMap()))
                .manager(getKeyType());
    }

    private @CheckForNull IOException findKeyException(Throwable ex) {
        final Class<? extends IOException> clazz = getKeyExceptionType();
        do {
            if (clazz.isInstance(ex)) return clazz.cast(ex);
        } while (null != (ex = ex.getCause()));
        return null;
    }

    @Override
    public final FsNode node(
            final BitField<FsAccessOption> options,
            final FsNodeName name)
    throws IOException {
        try {
            return controller.node(options, name);
        } catch (final ControlFlowException ex) {
            if (!name.isRoot() || null == findKeyException(ex)) throw ex;
            Entry node = getParent().node(
                    options, getModel()
                                 .getMountPoint()
                                 .getPath()
                                 .resolve(name)
                                 .getNodeName());
            // We're not holding any locks, so it's possible that someone else
            // has concurrently modified the parent file system.
            if (null == node) return null;
            // The entry is inaccessible for some reason.
            // This may be because the cipher key is not available.
            // Now mask the entry as a special file.
            if (node instanceof FsCovariantNode<?>)
                node = ((FsCovariantNode<?>) node).getEntry();
            final FsCovariantNode<FsArchiveEntry>
                    special = new FsCovariantNode<>(ROOT_PATH);
            special.put(SPECIAL, driver.newEntry(ROOT_PATH, SPECIAL, node));
            return special;
        }
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options, final FsNodeName name, final BitField<Access> types)
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
                                 .getNodeName(),
                    types);
        }
    }

    @Override
    public final void unlink(
            final BitField<FsAccessOption> options,
            final FsNodeName name)
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
        if (!fsu.equals(mpu) || name.isRoot()) getKeyManager().unlink(fsu);
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
        getKeyManager().release(driver.mountPointUri(getModel()));
        builder.check();
    }
}
