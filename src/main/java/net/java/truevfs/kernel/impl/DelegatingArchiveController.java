/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import bali.Cache;
import bali.Lookup;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static bali.CachingStrategy.NOT_THREAD_SAFE;

interface DelegatingArchiveController<E extends FsArchiveEntry> extends ArchiveController<E> {

    @Lookup(param = "controller")
    ArchiveController<E> getController();

    @Cache(NOT_THREAD_SAFE)
    @Override
    default ArchiveModel<E> getModel() {
        return getController().getModel();
    }

    @Cache(NOT_THREAD_SAFE)
    @Override
    default FsController getParent() {
        return getController().getParent();
    }

    @Override
    default Optional<? extends FsNode> node(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        return getController().node(options, name);
    }

    @Override
    default void checkAccess(BitField<FsAccessOption> options, FsNodeName name, BitField<Entry.Access> types) throws IOException {
        getController().checkAccess(options, name, types);
    }

    @Override
    default void setReadOnly(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        getController().setReadOnly(options, name);
    }

    @Override
    default boolean setTime(BitField<FsAccessOption> options, FsNodeName name, Map<Entry.Access, Long> times) throws IOException {
        return getController().setTime(options, name, times);
    }

    @Override
    default boolean setTime(BitField<FsAccessOption> options, FsNodeName name, BitField<Entry.Access> types, long time) throws IOException {
        return getController().setTime(options, name, types, time);
    }

    @Override
    default InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name) {
        return getController().input(options, name);
    }

    @Override
    default OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsNodeName name, Optional<? extends Entry> template) {
        return getController().output(options, name, template);
    }

    @Override
    default void make(BitField<FsAccessOption> options, FsNodeName name, Entry.Type type, Optional<? extends Entry> template) throws IOException {
        getController().make(options, name, type, template);
    }

    @Override
    default void unlink(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        getController().unlink(options, name);
    }

    @Override
    default void sync(BitField<FsSyncOption> options) throws FsSyncException {
        getController().sync(options);
    }
}
