/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.ext.throttle;

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
import javax.swing.Icon;

/**
 * Calls back the given {@link ThrottleManager} before each file system
 * operation in order to register itself as the most recently used archive file
 * system and syncs the least recently used archive file systems which exceed
 * {@link ThrottleManager#getMaximumOfMostRecentlyUsedArchiveFiles()} before proceeding with the file
 * system operation.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
final class ThrottleController
extends FsDecoratingController<FsModel, FsController<?>> {

    private final ThrottleManager manager;

    ThrottleController(
            final ThrottleManager manager,
            final FsController<?> controller) {
        super(controller);
        assert null != manager;
        assert null != controller.getParent();
        this.manager = manager;
    }

    void accessMruAndSyncLru() throws FsSyncException {
        manager.accessMru(this).syncLru();
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        accessMruAndSyncLru();
        return delegate.getOpenIcon();
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        accessMruAndSyncLru();
        return delegate.getClosedIcon();
    }

    @Override
    public boolean isReadOnly() throws IOException {
        accessMruAndSyncLru();
        return delegate.isReadOnly();
    }

    @Override
    public FsEntry getEntry(FsEntryName name) throws IOException {
        accessMruAndSyncLru();
        return delegate.getEntry(name);
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        accessMruAndSyncLru();
        return delegate.isReadable(name);
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        accessMruAndSyncLru();
        return delegate.isWritable(name);
    }

    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        accessMruAndSyncLru();
        return delegate.isExecutable(name);
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        accessMruAndSyncLru();
        delegate.setReadOnly(name);
    }

    @Override
    public boolean setTime(FsEntryName name, Map<Access, Long> times, BitField<FsOutputOption> options) throws IOException {
        accessMruAndSyncLru();
        return delegate.setTime(name, times, options);
    }

    @Override
    public boolean setTime(FsEntryName name, BitField<Access> types, long value, BitField<FsOutputOption> options) throws IOException {
        accessMruAndSyncLru();
        return delegate.setTime(name, types, value, options);
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
                accessMruAndSyncLru();
                return getBoundSocket().getLocalTarget();
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                accessMruAndSyncLru();
                return getBoundSocket().newReadOnlyFile();
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                accessMruAndSyncLru();
                return getBoundSocket().newSeekableByteChannel();
            }

            @Override
            public InputStream newInputStream() throws IOException {
                accessMruAndSyncLru();
                return getBoundSocket().newInputStream();
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
                accessMruAndSyncLru();
                return getBoundSocket().getLocalTarget();
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                accessMruAndSyncLru();
                return getBoundSocket().newSeekableByteChannel();
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                accessMruAndSyncLru();
                return getBoundSocket().newOutputStream();
            }
        } // Output
        return new Output();
    }

    @Override
    public void mknod(FsEntryName name, Type type, BitField<FsOutputOption> options, Entry template) throws IOException {
        accessMruAndSyncLru();
        delegate.mknod(name, type, options, template);
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options) throws IOException {
        accessMruAndSyncLru();
        delegate.unlink(name, options);
    }
}
