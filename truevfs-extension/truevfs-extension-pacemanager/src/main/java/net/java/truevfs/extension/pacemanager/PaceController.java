/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.pacemanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.*;
import net.java.truevfs.kernel.spec.cio.Entry.Access;
import net.java.truevfs.kernel.spec.cio.Entry.Type;
import net.java.truevfs.kernel.spec.cio.*;

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
extends FsDecoratingController<FsModel, FsController<FsModel>> {

    private final PaceManagerController manager;

    PaceController(
            final PaceManagerController manager,
            final FsController<FsModel> controller) {
        super(controller);
        assert null != manager;
        assert null != controller.getParent();
        this.manager = manager;
    }

    @Override
    public @CheckForNull FsEntry stat(
            final BitField<FsAccessOption> options,
            final FsEntryName name)
    throws IOException {
        final FsController<FsModel> c = this.controller;
        manager.retain(c);
        final FsEntry result = c.stat(options, name);
        manager.accessed(c);
        return result;
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final BitField<Access> types)
    throws IOException {
        final FsController<FsModel> c = this.controller;
        manager.retain(c);
        c.checkAccess(options, name, types);
        manager.accessed(c);
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        final FsController<FsModel> c = this.controller;
        manager.retain(c);
        c.setReadOnly(name);
        manager.accessed(c);
    }

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final Map<Access, Long> times)
    throws IOException {
        final FsController<FsModel> c = this.controller;
        manager.retain(c);
        final boolean result = c.setTime(options, name, times);
        manager.accessed(c);
        return result;
    }

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final BitField<Access> types,
            final long value)
    throws IOException {
        final FsController<FsModel> c = this.controller;
        manager.retain(c);
        final boolean result = c.setTime(options, name, types, value);
        manager.accessed(c);
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
                final FsController<FsModel> c = PaceController.this.controller;
                manager.retain(c);
                final Entry result = socket().target();
                manager.accessed(c);
                return result;
            }

            @Override
            public SeekableByteChannel channel(@CheckForNull OutputSocket<? extends Entry> peer) throws IOException {
                final FsController<FsModel> c = PaceController.this.controller;
                manager.retain(c);
                final SeekableByteChannel result = socket().channel(peer);
                manager.accessed(c);
                return result;
            }

            @Override
            public InputStream stream(@CheckForNull OutputSocket<? extends Entry> peer) throws IOException {
                final FsController<FsModel> c = PaceController.this.controller;
                manager.retain(c);
                final InputStream result = socket().stream(peer);
                manager.accessed(c);
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
                final FsController<FsModel> c = PaceController.this.controller;
                manager.retain(c);
                final Entry result = socket().target();
                manager.accessed(c);
                return result;
            }

            @Override
            public SeekableByteChannel channel(@CheckForNull InputSocket<? extends Entry> peer) throws IOException {
                final FsController<FsModel> c = PaceController.this.controller;
                manager.retain(c);
                final SeekableByteChannel result = socket().channel(peer);
                manager.accessed(c);
                return result;
            }

            @Override
            public OutputStream stream(@CheckForNull InputSocket<? extends Entry> peer) throws IOException {
                final FsController<FsModel> c = PaceController.this.controller;
                manager.retain(c);
                final OutputStream result = socket().stream(peer);
                manager.accessed(c);
                return result;
            }
        } // Output
        return new Output();
    }

    @Override
    public void mknod(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final Type type,
            final Entry template)
    throws IOException {
        final FsController<FsModel> c = this.controller;
        manager.retain(c);
        c.mknod(options, name, type, template);
        manager.accessed(c);
    }

    @Override
    public void unlink(
            final BitField<FsAccessOption> options,
            final FsEntryName name)
    throws IOException {
        final FsController<FsModel> c = this.controller;
        manager.retain(c);
        c.unlink(options, name);
        manager.accessed(c);
    }
}
