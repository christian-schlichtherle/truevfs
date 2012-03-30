/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.fs.addr.FsEntryName;
import de.truezip.kernel.fs.option.FsAccessOption;
import de.truezip.kernel.fs.option.FsSyncOption;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
import java.io.IOException;
import java.util.Map;
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

    /** The decorated file system controller. */
    protected final C delegate;

    /**
     * Constructs a new decorating file system controller.
     *
     * @param controller the decorated file system controller.
     */
    protected FsDecoratingController(final C controller) {
        super(controller.getModel());
        this.delegate = controller;
    }

    @Override
    public FsController<?> getParent() {
        return delegate.getParent();
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return delegate.isReadOnly();
    }

    @Override
    public FsEntry getEntry(FsEntryName name)
    throws IOException {
        return delegate.getEntry(name);
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        return delegate.isReadable(name);
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        return delegate.isWritable(name);
    }

    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        return delegate.isExecutable(name);
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        delegate.setReadOnly(name);
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<FsAccessOption> options)
    throws IOException {
        return delegate.setTime(name, times, options);
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Access> types,
            long value,
            BitField<FsAccessOption> options)
    throws IOException {
        return delegate.setTime(name, types, value, options);
    }

    @Override
    public InputSocket<?>
    getInputSocket( FsEntryName name,
                    BitField<FsAccessOption> options) {
        return delegate.getInputSocket(name, options);
    }

    @Override
    public OutputSocket<?>
    getOutputSocket(    FsEntryName name,
                        BitField<FsAccessOption> options,
                        Entry template) {
        return delegate.getOutputSocket(name, options, template);
    }

    @Override
    public void
    mknod(  FsEntryName name,
            Type type,
            BitField<FsAccessOption> options,
            Entry template)
    throws IOException {
        delegate.mknod(name, type, options, template);
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsAccessOption> options)
    throws IOException {
        delegate.unlink(name, options);
    }

    @Override
    public <X extends IOException> void
    sync(   BitField<FsSyncOption> options,
            ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        delegate.sync(options, handler);
    }

    @Override
    public String toString() {
        return String.format("%s[delegate=%s]",
                getClass().getName(),
                delegate);
    }
}
