/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.extension.pace;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Calls back the given {@link PaceManager} before and after each file system
 * operation in order to {@linkplain PaceManager#sync sync} the least recently
 * touched file systems which exceed the property
 * {@link PaceManager#getMaximumFileSystemsMounted()}
 * and register itself as the most recently touched file system.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
final class PaceController
extends FsDecoratingController<FsModel, FsController<?>> {

    private final PaceManager manager;

    PaceController(
            final PaceManager manager,
            final FsController<?> controller) {
        super(controller);
        assert null != manager;
        assert null != controller.getParent();
        this.manager = manager;
    }

    @Override
    public boolean isReadOnly() throws IOException {
        manager.retain(this);
        final boolean result = delegate.isReadOnly();
        manager.accessed(this);
        return result;
    }

    @Override
    public FsEntry getEntry(FsEntryName name) throws IOException {
        manager.retain(this);
        final FsEntry result = delegate.getEntry(name);
        manager.accessed(this);
        return result;
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        manager.retain(this);
        final boolean result = delegate.isReadable(name);
        manager.accessed(this);
        return result;
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        manager.retain(this);
        final boolean result = delegate.isWritable(name);
        manager.accessed(this);
        return result;
    }

    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        manager.retain(this);
        final boolean result = delegate.isExecutable(name);
        manager.accessed(this);
        return result;
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        manager.retain(this);
        delegate.setReadOnly(name);
        manager.accessed(this);
    }

    @Override
    public boolean setTime(FsEntryName name, Map<Access, Long> times, BitField<FsOutputOption> options) throws IOException {
        manager.retain(this);
        final boolean result = delegate.setTime(name, times, options);
        manager.accessed(this);
        return result;
    }

    @Override
    public boolean setTime(FsEntryName name, BitField<Access> types, long value, BitField<FsOutputOption> options) throws IOException {
        manager.retain(this);
        final boolean result = delegate.setTime(name, types, value, options);
        manager.accessed(this);
        return result;
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsInputOption> options) {
        @NotThreadSafe
        final class Input extends DecoratingInputSocket<Entry> {
            Input() { super(delegate.getInputSocket(name, options)); }

            @Override
            public Entry getLocalTarget() throws IOException {
                manager.retain(PaceController.this);
                final Entry result = getBoundSocket().getLocalTarget();
                manager.accessed(PaceController.this);
                return result;
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                manager.retain(PaceController.this);
                final ReadOnlyFile result = getBoundSocket().newReadOnlyFile();
                manager.accessed(PaceController.this);
                return result;
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                manager.retain(PaceController.this);
                final SeekableByteChannel result = getBoundSocket().newSeekableByteChannel();
                manager.accessed(PaceController.this);
                return result;
            }

            @Override
            public InputStream newInputStream() throws IOException {
                manager.retain(PaceController.this);
                final InputStream result = getBoundSocket().newInputStream();
                manager.accessed(PaceController.this);
                return result;
            }
        } // Input
        return new Input();
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsOutputOption> options,
            final Entry template) {
        @NotThreadSafe
        final class Output extends DecoratingOutputSocket<Entry> {
            Output() { super(delegate.getOutputSocket(name, options, template)); }

            @Override
            public Entry getLocalTarget() throws IOException {
                manager.retain(PaceController.this);
                final Entry result = getBoundSocket().getLocalTarget();
                manager.accessed(PaceController.this);
                return result;
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                manager.retain(PaceController.this);
                final SeekableByteChannel result = getBoundSocket().newSeekableByteChannel();
                manager.accessed(PaceController.this);
                return result;
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                manager.retain(PaceController.this);
                final OutputStream result = getBoundSocket().newOutputStream();
                manager.accessed(PaceController.this);
                return result;
            }
        } // Output
        return new Output();
    }

    @Override
    public void mknod(FsEntryName name, Type type, BitField<FsOutputOption> options, Entry template) throws IOException {
        manager.retain(this);
        delegate.mknod(name, type, options, template);
        manager.accessed(this);
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options) throws IOException {
        manager.retain(this);
        delegate.unlink(name, options);
        manager.accessed(this);
    }
}
