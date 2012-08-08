/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.IOException;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.Entry.Access;
import net.java.truevfs.kernel.spec.cio.Entry.Type;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * An abstract decorator for a file system controller.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsDecoratingController extends FsAbstractController {

    /** The decorated file system controller. */
    protected final FsController controller;

    protected FsDecoratingController(final FsController controller) {
        super(controller.getModel());
        this.controller = controller;
    }

    @Override
    public FsController getParent() {
        return controller.getParent();
    }

    @Override
    public @CheckForNull FsNode stat(
            BitField<FsAccessOption> options,
            FsNodeName name)
    throws IOException {
        return controller.stat(options, name);
    }

    @Override
    public void checkAccess(
            BitField<FsAccessOption> options,
            FsNodeName name,
            BitField<Access> types)
    throws IOException {
        controller.checkAccess(options, name, types);
    }

    @Override
    public void setReadOnly(FsNodeName name) throws IOException {
        controller.setReadOnly(name);
    }

    @Override
    public boolean setTime(
            BitField<FsAccessOption> options,
            FsNodeName name,
            Map<Access, Long> times)
    throws IOException {
        return controller.setTime(options, name, times);
    }

    @Override
    public boolean setTime(
            BitField<FsAccessOption> options,
            FsNodeName name,
            BitField<Access> types,
            long value)
    throws IOException {
        return controller.setTime(options, name, types, value);
    }

    @Override
    public InputSocket<? extends Entry> input(
            BitField<FsAccessOption> options,
            FsNodeName name) {
        return controller.input(options, name);
    }

    @Override
    public OutputSocket<? extends Entry> output(
            BitField<FsAccessOption> options,
            FsNodeName name,
            @CheckForNull Entry template) {
        return controller.output(options, name, template);
    }

    @Override
    public void mknod(
            BitField<FsAccessOption> options,
            FsNodeName name,
            Type type,
            @CheckForNull Entry template)
    throws IOException {
        controller.mknod(options, name, type, template);
    }

    @Override
    public void unlink(
            BitField<FsAccessOption> options,
            FsNodeName name)
    throws IOException {
        controller.unlink(options, name);
    }

    @Override
    public void sync(BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        controller.sync(options);
    }

    @Override
    public String toString() {
        return String.format("%s[controller=%s]",
                getClass().getName(),
                controller);
    }
}
