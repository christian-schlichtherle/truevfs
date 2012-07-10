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
 * Calls back the given {@link PaceManager} before each file system
 * operation in order to register itself as the most recently used archive file
 * system and syncs the least recently used archive file systems which exceed
 * {@link PaceManager#getMaximumOfMostRecentlyUsedArchiveFiles()} before proceeding with the file
 * system operation.
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
        manager.syncLru(this);
        final boolean result = delegate.isReadOnly();
        manager.accessedMru(this);
        return result;
    }

    @Override
    public FsEntry getEntry(FsEntryName name) throws IOException {
        manager.syncLru(this);
        final FsEntry result = delegate.getEntry(name);
        manager.accessedMru(this);
        return result;
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        manager.syncLru(this);
        final boolean result = delegate.isReadable(name);
        manager.accessedMru(this);
        return result;
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        manager.syncLru(this);
        final boolean result = delegate.isWritable(name);
        manager.accessedMru(this);
        return result;
    }

    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        manager.syncLru(this);
        final boolean result = delegate.isExecutable(name);
        manager.accessedMru(this);
        return result;
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        manager.syncLru(this);
        delegate.setReadOnly(name);
        manager.accessedMru(this);
    }

    @Override
    public boolean setTime(FsEntryName name, Map<Access, Long> times, BitField<FsOutputOption> options) throws IOException {
        manager.syncLru(this);
        final boolean result = delegate.setTime(name, times, options);
        manager.accessedMru(this);
        return result;
    }

    @Override
    public boolean setTime(FsEntryName name, BitField<Access> types, long value, BitField<FsOutputOption> options) throws IOException {
        manager.syncLru(this);
        final boolean result = delegate.setTime(name, types, value, options);
        manager.accessedMru(this);
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
                manager.syncLru(PaceController.this);
                final Entry result = getBoundSocket().getLocalTarget();
                manager.accessedMru(PaceController.this);
                return result;
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                manager.syncLru(PaceController.this);
                final ReadOnlyFile result = getBoundSocket().newReadOnlyFile();
                manager.accessedMru(PaceController.this);
                return result;
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                manager.syncLru(PaceController.this);
                final SeekableByteChannel result = getBoundSocket().newSeekableByteChannel();
                manager.accessedMru(PaceController.this);
                return result;
            }

            @Override
            public InputStream newInputStream() throws IOException {
                manager.syncLru(PaceController.this);
                final InputStream result = getBoundSocket().newInputStream();
                manager.accessedMru(PaceController.this);
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
                manager.syncLru(PaceController.this);
                final Entry result = getBoundSocket().getLocalTarget();
                manager.accessedMru(PaceController.this);
                return result;
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                manager.syncLru(PaceController.this);
                final SeekableByteChannel result = getBoundSocket().newSeekableByteChannel();
                manager.accessedMru(PaceController.this);
                return result;
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                manager.syncLru(PaceController.this);
                final OutputStream result = getBoundSocket().newOutputStream();
                manager.accessedMru(PaceController.this);
                return result;
            }
        } // Output
        return new Output();
    }

    @Override
    public void mknod(FsEntryName name, Type type, BitField<FsOutputOption> options, Entry template) throws IOException {
        manager.syncLru(this);
        delegate.mknod(name, type, options, template);
        manager.accessedMru(this);
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options) throws IOException {
        manager.syncLru(this);
        delegate.unlink(name, options);
        manager.accessedMru(this);
    }
}
