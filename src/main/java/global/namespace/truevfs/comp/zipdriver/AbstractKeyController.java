/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.zipdriver;

import lombok.val;
import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.Entry.Access;
import global.namespace.truevfs.comp.key.spec.KeyManager;
import global.namespace.truevfs.comp.key.spec.KeyManagerMap;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.comp.shed.ControlFlowException;
import global.namespace.truevfs.kernel.spec.*;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import static global.namespace.truevfs.comp.cio.Entry.Type.SPECIAL;
import static global.namespace.truevfs.kernel.spec.FsNodeName.ROOT;

/**
 * This file system controller decorates another file system controller in
 * order to manage the keys required for accessing encrypted ZIP files.
 *
 * @author Christian Schlichtherle
 */
public abstract class AbstractKeyController extends FsDecoratingController {

    private static final String ROOT_PATH = ROOT.getPath();

    protected final AbstractZipDriver<?> driver;

    private volatile KeyManagerMap keyManagerMap;

    /**
     * Constructs a new key manager controller.
     *
     * @param controller the non-{@code null} file system controller to
     *                   decorate.
     * @param driver     the ZIP driver.
     */
    protected AbstractKeyController(
            final FsController controller,
            final AbstractZipDriver<?> driver) {
        super(controller);
        this.driver = Objects.requireNonNull(driver);
    }

    protected abstract Class<?> getKeyType();

    protected abstract Class<? extends IOException> getKeyExceptionType();

    private Optional<? extends IOException> findKeyException(Throwable ex) {
        val clazz = getKeyExceptionType();
        do {
            if (clazz.isInstance(ex)) {
                return Optional.of(clazz.cast(ex));
            }
        } while (null != (ex = ex.getCause()));
        return Optional.empty();
    }

    @Override
    public final Optional<? extends FsNode> node(
            final BitField<FsAccessOption> options,
            final FsNodeName name
    ) throws IOException {
        try {
            return controller.node(options, name);
        } catch (final ControlFlowException ex) {
            if (!name.isRoot() || !findKeyException(ex).isPresent()) {
                throw ex;
            }
            val op = getParent();
            assert op.isPresent();
            val p = op.get();
            val ompp = getMountPoint().getPath();
            assert ompp.isPresent();
            val mpp = ompp.get();
            val on = p.node(options, mpp.resolve(name).getNodeName());
            if (on.isPresent()) {
                Entry n = on.get();
                // The entry is inaccessible for some reason.
                // This may be because the cipher key is not available.
                // Now mask the entry as a special file:
                if (n instanceof FsCovariantNode<?>) {
                    n = ((FsCovariantNode<?>) n).getEntry();
                }
                final FsCovariantNode<FsArchiveEntry> special = new FsCovariantNode<>(ROOT_PATH);
                special.put(SPECIAL, driver.newEntry(ROOT_PATH, SPECIAL, n));
                return Optional.of(special);
            }
            // We're not holding any locks, so it's possible that someone else has concurrently modified the parent file
            // system.
            return Optional.empty();
        }
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final BitField<Access> types
    ) throws IOException {
        try {
            controller.checkAccess(options, name, types);
        } catch (final ControlFlowException ex) {
            if (!name.isRoot() || !findKeyException(ex).isPresent()) {
                throw ex;
            }
            val op = getParent();
            assert op.isPresent();
            val p = op.get();
            val ompp = getMountPoint().getPath();
            assert ompp.isPresent();
            val mpp = ompp.get();
            p.checkAccess(options, mpp.resolve(name).getNodeName(), types);
        }
    }

    @Override
    public final void unlink(
            final BitField<FsAccessOption> options,
            final FsNodeName name
    ) throws IOException {
        try {
            controller.unlink(options, name);
        } catch (final ControlFlowException ex) {
            // If the exception is caused by a key exception, then throw this
            // cause instead in order to avoid treating the target archive file
            // like a false positive and routing this operation to the parent
            // file system.
            // This prevents the application from inadvertently deleting an
            // encrypted ZIP file just because the user cancelled key prompting.
            final Optional<? extends IOException> keyEx = findKeyException(ex);
            if (keyEx.isPresent()) {
                throw keyEx.get();
            } else {
                throw ex;
            }
        }
        final FsModel model = getModel();
        final URI mpu = driver.mountPointUri(model);
        final URI fsu = driver.fileSystemUri(model, name.toString());
        if (!fsu.equals(mpu) || name.isRoot())
            keyManager().unlink(fsu);
    }

    @Override
    public void sync(final BitField<FsSyncOption> options) throws FsSyncException {
        final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
        try {
            controller.sync(options);
        } catch (FsSyncWarningException ex) {
            builder.warn(ex);
        }
        keyManager().release(driver.mountPointUri(getModel()));
        builder.check();
    }

    private KeyManager<?> keyManager() {
        return keyManagerMap().manager(getKeyType());
    }

    private KeyManagerMap keyManagerMap() {
        final KeyManagerMap keyManagerMap = this.keyManagerMap;
        return null != keyManagerMap ? keyManagerMap : (this.keyManagerMap = driver.getKeyManagerMap());
    }
}
