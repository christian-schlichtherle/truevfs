/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import lombok.Getter;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

abstract class DecoratingArchiveController<E extends FsArchiveEntry> extends AbstractArchiveController<E> {

    final ArchiveController<E> controller;

    @Getter(lazy = true)
    private final ArchiveModel<E> model = controller.getModel();

    @Getter(lazy = true)
    private final ReentrantReadWriteLock lock = controller.getLock();

    DecoratingArchiveController(final ArchiveController<E> controller) {
        this.controller = controller;
        assert controller.getLock() == controller.getModel().getLock();
    }

    @Override
    public Optional<? extends FsNode> node(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        return controller.node(options, name);
    }

    @Override
    public void checkAccess(BitField<FsAccessOption> options, FsNodeName name, BitField<Entry.Access> types) throws IOException {
        controller.checkAccess(options, name, types);
    }

    @Override
    public void setReadOnly(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        controller.setReadOnly(options, name);
    }

    @Override
    public boolean setTime(BitField<FsAccessOption> options, FsNodeName name, Map<Entry.Access, Long> times) throws IOException {
        return controller.setTime(options, name, times);
    }

    @Override
    public boolean setTime(BitField<FsAccessOption> options, FsNodeName name, BitField<Entry.Access> types, long time) throws IOException {
        return controller.setTime(options, name, types, time);
    }

    @Override
    public InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name) {
        return controller.input(options, name);
    }

    @Override
    public OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsNodeName name, Optional<Entry> template) {
        return controller.output(options, name, template);
    }

    @Override
    public void make(BitField<FsAccessOption> options, FsNodeName name, Entry.Type type, Optional<Entry> template) throws IOException {
        controller.make(options, name, type, template);
    }

    @Override
    public void unlink(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        controller.unlink(options, name);
    }

    @Override
    public void sync(BitField<FsSyncOption> options) throws FsSyncException {
        controller.sync(options);
    }
}
