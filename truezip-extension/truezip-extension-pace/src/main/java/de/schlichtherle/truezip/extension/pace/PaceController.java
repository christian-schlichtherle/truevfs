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
 * Calls back the given {@link PaceManagerController} before and after each
 * file system operation in order to
 * {@linkplain PaceManagerController#sync sync} the least recently accessed
 * file systems which exceed the property
 * {@link PaceManagerController#getMaximumFileSystemsMounted()}
 * and register itself as the most recently accessed file system.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
final class PaceController
extends FsDecoratingController<FsModel, FsController<? extends FsModel>> {

    private final PaceManagerController manager;

    PaceController(
            final PaceManagerController manager,
            final FsController<?> controller) {
        super(controller);
        assert null != manager;
        assert null != controller.getParent();
        this.manager = manager;
    }

    @Override
    public boolean isReadOnly() throws IOException {
        final FsController<? extends FsModel> c = this.delegate;
        manager.retain(c);
        final boolean result = c.isReadOnly();
        manager.accessed(c);
        return result;
    }

    @Override
    public FsEntry getEntry(FsEntryName name) throws IOException {
        final FsController<? extends FsModel> c = this.delegate;
        manager.retain(c);
        final FsEntry result = c.getEntry(name);
        manager.accessed(c);
        return result;
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        final FsController<? extends FsModel> c = this.delegate;
        manager.retain(c);
        final boolean result = c.isReadable(name);
        manager.accessed(c);
        return result;
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        final FsController<? extends FsModel> c = this.delegate;
        manager.retain(c);
        final boolean result = c.isWritable(name);
        manager.accessed(c);
        return result;
    }

    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        final FsController<? extends FsModel> c = this.delegate;
        manager.retain(c);
        final boolean result = c.isExecutable(name);
        manager.accessed(c);
        return result;
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        final FsController<? extends FsModel> c = this.delegate;
        manager.retain(c);
        c.setReadOnly(name);
        manager.accessed(c);
    }

    @Override
    public boolean setTime(FsEntryName name, Map<Access, Long> times, BitField<FsOutputOption> options) throws IOException {
        final FsController<? extends FsModel> c = this.delegate;
        manager.retain(c);
        final boolean result = c.setTime(name, times, options);
        manager.accessed(c);
        return result;
    }

    @Override
    public boolean setTime(FsEntryName name, BitField<Access> types, long value, BitField<FsOutputOption> options) throws IOException {
        final FsController<? extends FsModel> c = this.delegate;
        manager.retain(c);
        final boolean result = c.setTime(name, types, value, options);
        manager.accessed(c);
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
                final FsController<? extends FsModel> c = PaceController.this.delegate;
                manager.retain(c);
                final Entry result = getBoundSocket().getLocalTarget();
                manager.accessed(c);
                return result;
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                final FsController<? extends FsModel> c = PaceController.this.delegate;
                manager.retain(c);
                final ReadOnlyFile result = getBoundSocket().newReadOnlyFile();
                manager.accessed(c);
                return result;
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                final FsController<? extends FsModel> c = PaceController.this.delegate;
                manager.retain(c);
                final SeekableByteChannel result = getBoundSocket().newSeekableByteChannel();
                manager.accessed(c);
                return result;
            }

            @Override
            public InputStream newInputStream() throws IOException {
                final FsController<? extends FsModel> c = PaceController.this.delegate;
                manager.retain(c);
                final InputStream result = getBoundSocket().newInputStream();
                manager.accessed(c);
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
                final FsController<? extends FsModel> c = PaceController.this.delegate;
                manager.retain(c);
                final Entry result = getBoundSocket().getLocalTarget();
                manager.accessed(c);
                return result;
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                final FsController<? extends FsModel> c = PaceController.this.delegate;
                manager.retain(c);
                final SeekableByteChannel result = getBoundSocket().newSeekableByteChannel();
                manager.accessed(c);
                return result;
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                final FsController<? extends FsModel> c = PaceController.this.delegate;
                manager.retain(c);
                final OutputStream result = getBoundSocket().newOutputStream();
                manager.accessed(c);
                return result;
            }
        } // Output
        return new Output();
    }

    @Override
    public void mknod(FsEntryName name, Type type, BitField<FsOutputOption> options, Entry template) throws IOException {
        final FsController<? extends FsModel> c = this.delegate;
        manager.retain(c);
        c.mknod(name, type, options, template);
        manager.accessed(c);
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options) throws IOException {
        final FsController<? extends FsModel> c = this.delegate;
        manager.retain(c);
        c.unlink(name, options);
        manager.accessed(c);
    }
}
