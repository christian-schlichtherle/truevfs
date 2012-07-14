/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.pace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import net.truevfs.kernel.spec.*;
import net.truevfs.kernel.spec.cio.Entry.Access;
import net.truevfs.kernel.spec.cio.Entry.Type;
import net.truevfs.kernel.spec.cio.*;
import net.truevfs.kernel.spec.util.BitField;

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
extends FsDecoratingController<FsModel, FsController<? extends FsModel>> {

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
    public @CheckForNull FsEntry stat(
            BitField<FsAccessOption> options,
            FsEntryName name)
    throws IOException {
        manager.retain(this);
        final FsEntry result = controller.stat(options, name);
        manager.accessed(this);
        return result;
    }

    @Override
    public void checkAccess(
            BitField<FsAccessOption> options,
            FsEntryName name,
            BitField<Access> types)
    throws IOException {
        manager.retain(this);
        controller.checkAccess(options, name, types);
        manager.accessed(this);
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        manager.retain(this);
        controller.setReadOnly(name);
        manager.accessed(this);
    }

    @Override
    public boolean setTime(
            BitField<FsAccessOption> options,
            FsEntryName name,
            Map<Access, Long> times)
    throws IOException {
        manager.retain(this);
        final boolean result = controller.setTime(options, name, times);
        manager.accessed(this);
        return result;
    }

    @Override
    public boolean setTime(
            BitField<FsAccessOption> options,
            FsEntryName name,
            BitField<Access> types,
            long value)
    throws IOException {
        manager.retain(this);
        final boolean result = controller.setTime(options, name, types, value);
        manager.accessed(this);
        return result;
    }

    @Override
    public InputSocket<? extends Entry> input(
            final BitField<FsAccessOption> options,
            final FsEntryName name) {
        @NotThreadSafe
        final class Input extends DecoratingInputSocket<Entry> {
            Input() { super(controller.input(options, name)); }

            @Override
            public Entry target() throws IOException {
                manager.retain(PaceController.this);
                final Entry result = socket.target();
                manager.accessed(PaceController.this);
                return result;
            }

            @Override
            public InputStream stream(OutputSocket<? extends Entry> peer)
            throws IOException {
                manager.retain(PaceController.this);
                final InputStream result = socket.stream(peer);
                manager.accessed(PaceController.this);
                return result;
            }

            @Override
            public SeekableByteChannel channel(OutputSocket<? extends Entry> peer)
            throws IOException {
                manager.retain(PaceController.this);
                final SeekableByteChannel result = socket.channel(peer);
                manager.accessed(PaceController.this);
                return result;
            }
        } // Input
        return new Input();
    }

    @Override
    public OutputSocket<? extends Entry> output(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final Entry template) {
        @NotThreadSafe
        final class Output extends DecoratingOutputSocket<Entry> {
            Output() { super(controller.output(options, name, template)); }

            @Override
            public Entry target() throws IOException {
                manager.retain(PaceController.this);
                final Entry result = socket.target();
                manager.accessed(PaceController.this);
                return result;
            }

            @Override
            public OutputStream stream(InputSocket<? extends Entry> peer)
            throws IOException {
                manager.retain(PaceController.this);
                final OutputStream result = socket.stream(peer);
                manager.accessed(PaceController.this);
                return result;
            }

            @Override
            public SeekableByteChannel channel(InputSocket<? extends Entry> peer)
            throws IOException {
                manager.retain(PaceController.this);
                final SeekableByteChannel result = socket.channel(peer);
                manager.accessed(PaceController.this);
                return result;
            }
        } // Output
        return new Output();
    }

    @Override
    public void mknod(
            BitField<FsAccessOption> options,
            FsEntryName name,
            Type type,
            Entry template)
    throws IOException {
        manager.retain(this);
        controller.mknod(options, name, type, template);
        manager.accessed(this);
    }

    @Override
    public void unlink(
            BitField<FsAccessOption> options,
            FsEntryName name)
    throws IOException {
        manager.retain(this);
        controller.unlink(options, name);
        manager.accessed(this);
    }
}
