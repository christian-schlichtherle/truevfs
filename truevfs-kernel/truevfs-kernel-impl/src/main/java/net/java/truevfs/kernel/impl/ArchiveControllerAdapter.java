/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.*;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

final class ArchiveControllerAdapter extends FsAbstractController {

    private final FsController parent;
    private final ArchiveController<?> controller;

    ArchiveControllerAdapter(FsController parent, ArchiveController<?> controller) {
        super(controller.getModel());
        this.parent = parent;
        this.controller = controller;
    }

    @Override
    public FsController getParent() {
        return parent;
    }

    @Override
    public FsNode node(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        return controller.node(options, name).orElse(null);
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
    public boolean setTime(BitField<FsAccessOption> options, FsNodeName name, BitField<Entry.Access> types, long value) throws IOException {
        return controller.setTime(options, name, types, value);
    }

    @Override
    public InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name) {
        return controller.input(options, name);
    }

    @Override
    public OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsNodeName name, @CheckForNull Entry template) {
        return controller.output(options, name, Optional.ofNullable(template));
    }

    @Override
    public void make(BitField<FsAccessOption> options, FsNodeName name, Entry.Type type, @CheckForNull Entry template) throws IOException {
        controller.make(options, name, type, Optional.ofNullable(template));
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
