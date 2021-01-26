/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import bali.Lookup;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.Entry.Access;
import net.java.truecommons.cio.Entry.Type;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.shed.BitField;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.Map;

/**
 * A file system controller which delegates its calls to another file system controller.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public interface FsDelegatingController extends FsController {

    /**
     * The underlying file system controller.
     */
    @Lookup(param = "controller")
    FsController getController();

    @Override
    default FsController getParent() {
        return getController().getParent();
    }

    @Override
    default FsModel getModel() {
        return getController().getModel();
    }

    /**
     * Returns the mount point of this (virtual) file system as
     * defined by the {@linkplain #getModel() model}.
     *
     * @return The mount point of this (virtual) file system as
     * defined by the {@linkplain #getModel() model}.
     */
    default FsMountPoint getMountPoint() {
        return getModel().getMountPoint();
    }

    /**
     * Returns the {@code mounted} property of the
     * {@linkplain #getModel() file system model}.
     *
     * @return the {@code mounted} property of the
     * {@linkplain #getModel() file system model}.
     */
    default boolean isMounted() {
        return getModel().isMounted();
    }

    /**
     * Sets the {@code mounted} property of the
     * {@linkplain #getModel() file system model}.
     *
     * @param mounted the {@code mounted} property of the
     *                {@linkplain #getModel() file system model}.
     */
    default void setMounted(boolean mounted) {
        getModel().setMounted(mounted);
    }

    @Override
    default @CheckForNull
    FsNode node(
            BitField<FsAccessOption> options,
            FsNodeName name)
            throws IOException {
        return getController().node(options, name);
    }

    @Override
    default void checkAccess(
            BitField<FsAccessOption> options,
            FsNodeName name,
            BitField<Access> types)
            throws IOException {
        getController().checkAccess(options, name, types);
    }

    @Override
    default void setReadOnly(BitField<FsAccessOption> options, FsNodeName name)
            throws IOException {
        getController().setReadOnly(options, name);
    }

    @Override
    default boolean setTime(
            BitField<FsAccessOption> options,
            FsNodeName name,
            Map<Access, Long> times)
            throws IOException {
        return getController().setTime(options, name, times);
    }

    @Override
    default boolean setTime(
            BitField<FsAccessOption> options,
            FsNodeName name,
            BitField<Access> types,
            long value)
            throws IOException {
        return getController().setTime(options, name, types, value);
    }

    @Override
    default InputSocket<? extends Entry> input(
            BitField<FsAccessOption> options,
            FsNodeName name) {
        return getController().input(options, name);
    }

    @Override
    default OutputSocket<? extends Entry> output(
            BitField<FsAccessOption> options,
            FsNodeName name,
            @CheckForNull Entry template) {
        return getController().output(options, name, template);
    }

    @Override
    default void make(
            BitField<FsAccessOption> options,
            FsNodeName name,
            Type type,
            @CheckForNull Entry template)
            throws IOException {
        getController().make(options, name, type, template);
    }

    @Override
    default void unlink(
            BitField<FsAccessOption> options,
            FsNodeName name)
            throws IOException {
        getController().unlink(options, name);
    }

    @Override
    default void sync(BitField<FsSyncOption> options) throws FsSyncException {
        getController().sync(options);
    }
}
