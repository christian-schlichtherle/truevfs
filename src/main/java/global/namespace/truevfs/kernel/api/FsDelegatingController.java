/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api;

import bali.Lookup;
import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.Entry.Access;
import global.namespace.truevfs.comp.cio.Entry.Type;
import global.namespace.truevfs.comp.cio.InputSocket;
import global.namespace.truevfs.comp.cio.OutputSocket;
import global.namespace.truevfs.comp.util.BitField;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * A file system controller which delegates its calls to another file system controller.
 *
 * @author Christian Schlichtherle
 */
public interface FsDelegatingController extends FsController {

    /**
     * The underlying file system controller.
     */
    @Lookup(param = "controller")
    FsController getController();

    @Override
    default Optional<? extends FsController> getParent() {
        return getController().getParent();
    }

    @Override
    default FsModel getModel() {
        return getController().getModel();
    }

    @Override
    default Optional<? extends FsNode> node(
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
            Optional<? extends Entry> template) {
        return getController().output(options, name, template);
    }

    @Override
    default void make(
            BitField<FsAccessOption> options,
            FsNodeName name,
            Type type,
            Optional<? extends Entry> template)
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
