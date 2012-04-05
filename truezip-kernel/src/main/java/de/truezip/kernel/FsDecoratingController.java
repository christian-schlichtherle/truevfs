/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import de.truezip.kernel.addr.FsEntryName;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.option.AccessOption;
import de.truezip.kernel.option.SyncOption;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
import java.io.IOException;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An abstract decorator for a file system controller.
 *
 * @param  <M> the type of the file system model.
 * @param  <C> the type of the decorated file system controller.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsDecoratingController<
        M extends FsModel,
        C extends FsController<? extends M>>
extends FsController<M> {

    /** The nullable decorated file system controller. */
    protected @Nullable C controller;

    protected FsDecoratingController(final C controller) {
        this.controller = controller;
    }

    @Override
    public M getModel() {
        return controller.getModel();
    }

    @Override
    public FsController<?> getParent() {
        return controller.getParent();
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return controller.isReadOnly();
    }

    @Override
    public FsEntry getEntry(FsEntryName name)
    throws IOException {
        return controller.getEntry(name);
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        return controller.isReadable(name);
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        return controller.isWritable(name);
    }

    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        return controller.isExecutable(name);
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        controller.setReadOnly(name);
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<AccessOption> options)
    throws IOException {
        return controller.setTime(name, times, options);
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Access> types,
            long value,
            BitField<AccessOption> options)
    throws IOException {
        return controller.setTime(name, types, value, options);
    }

    @Override
    public InputSocket<?>
    getInputSocket( FsEntryName name,
                    BitField<AccessOption> options) {
        return controller.getInputSocket(name, options);
    }

    @Override
    public OutputSocket<?>
    getOutputSocket(    FsEntryName name,
                        BitField<AccessOption> options,
                        Entry template) {
        return controller.getOutputSocket(name, options, template);
    }

    @Override
    public void
    mknod(  FsEntryName name,
            Type type,
            BitField<AccessOption> options,
            Entry template)
    throws IOException {
        controller.mknod(name, type, options, template);
    }

    @Override
    public void unlink(FsEntryName name, BitField<AccessOption> options)
    throws IOException {
        controller.unlink(name, options);
    }

    @Override
    public <X extends IOException> void
    sync(   BitField<SyncOption> options,
            ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        controller.sync(options, handler);
    }

    @Override
    public String toString() {
        return String.format("%s[controller=%s]",
                getClass().getName(),
                controller);
    }
}
