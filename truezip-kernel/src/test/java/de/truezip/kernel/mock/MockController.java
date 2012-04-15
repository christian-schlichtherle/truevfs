/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.mock;

import de.truezip.kernel.*;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.ThrowingInputStream;
import de.truezip.kernel.io.ThrowingOutputStream;
import de.truezip.kernel.io.ThrowingSeekableChannel;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class MockController extends FsModelController<FsModel> {

    private final @Nullable FsController<?> parent;
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    private final ConcurrentMap<FsEntryName, IOEntry<?>>
            map = new ConcurrentHashMap<>();
    private final TestConfig config;
    private volatile @CheckForNull ThrowManager control;

    public MockController(FsModel model, @CheckForNull FsController<?> parent) {
        this(model, parent, null);
    }

    /**
     * Constructs a new mock controller.
     * 
     * @param model The file system model.
     * @param parent The parent file system controller.
     * @param config The mocking configuration.
     */
    public MockController(  final FsModel model,
                            final @CheckForNull FsController<?> parent,
                            final @CheckForNull TestConfig config) {
        super(model);
        assert null == model.getParent()
                ? null == parent
                : model.getParent().equals(parent.getModel());
        this.parent = parent;
        this.config = null != config ? config : TestConfig.get();
    }

    private ThrowManager getThrowControl() {
        final ThrowManager control = this.control;
        return null != control ? control : (this.control = config.getThrowControl());
    }

    private void checkAllExceptions(final Object thiz) throws IOException {
        getThrowControl().check(thiz, IOException.class);
        checkUndeclaredExceptions(this);
    }

    private void checkUndeclaredExceptions(final Object thiz) {
        getThrowControl().check(thiz, RuntimeException.class);
        getThrowControl().check(thiz, Error.class);
    }

    @Override
    public FsController<?> getParent() {
        checkUndeclaredExceptions(this);
        return parent;
    }

    @Override
    public boolean isReadOnly() throws IOException {
        checkAllExceptions(this);
        return false;
    }

    @Override
    public FsEntry entry(FsEntryName name) throws IOException {
        checkAllExceptions(this);
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        checkAllExceptions(this);
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        checkAllExceptions(this);
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        checkAllExceptions(this);
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        checkAllExceptions(this);
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<FsAccessOption> options)
    throws IOException {
        checkAllExceptions(this);
        assert null != name;
        assert null != times;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Access> types,
            long value,
            BitField<FsAccessOption> options)
    throws IOException {
        checkAllExceptions(this);
        assert null != name;
        assert null != types;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public InputSocket<?> input(
            final FsEntryName name,
            final BitField<FsAccessOption> options) {
        checkUndeclaredExceptions(this);
        assert null != name;
        assert null != options;

        class Input extends DelegatingInputSocket<Entry> {
            @Override
            protected InputSocket<? extends Entry> getSocket()
            throws IOException {
                checkAllExceptions(this);
                final IOEntry<?> buffer = map.get(name);
                if (null == buffer)
                    throw new NoSuchFileException(name.toString());
                return buffer.input();
            }

            @Override
            public SeekableByteChannel channel()
            throws IOException {
                return new ThrowingSeekableChannel(
                        getBoundSocket().channel(),
                        config.getThrowControl());
            }

            @Override
            public InputStream stream()
            throws IOException {
                return new ThrowingInputStream(
                        getBoundSocket().stream(),
                        config.getThrowControl());
            }
        } // Input

        return new Input();
    }

    @Override
    public OutputSocket<?> output(
            final FsEntryName name,
            final BitField<FsAccessOption> options,
            final Entry template) {
        checkUndeclaredExceptions(this);
        assert null != name;
        assert null != options;

        class Output extends DelegatingOutputSocket<Entry> {
            @Override
            protected OutputSocket<? extends Entry> getSocket()
            throws IOException {
                checkAllExceptions(this);
                final IOEntry<?> n = new ByteArrayIOBuffer(
                        name.toString(), config.getDataSize());
                IOEntry<?> o = map.get(name);
                if (null == o)
                    o = map.putIfAbsent(name, n);
                return (null != o ? o : n).output();
            }

            @Override
            public SeekableByteChannel channel()
            throws IOException {
                return new ThrowingSeekableChannel(
                        getBoundSocket().channel(),
                        config.getThrowControl());
            }

            @Override
            public OutputStream stream()
            throws IOException {
                return new ThrowingOutputStream(
                        getBoundSocket().stream(),
                        config.getThrowControl());
            }
        } // Output

        return new Output();
    }

    @Override
    public void mknod(  FsEntryName name,
                        Type type,
                        BitField<FsAccessOption> options,
                        Entry template)
    throws IOException {
        checkAllExceptions(this);
        assert null != name;
        assert null != type;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsAccessOption> options)
    throws IOException {
        checkAllExceptions(this);
        assert null != name;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        getThrowControl().check(this, FsSyncException.class);
        checkUndeclaredExceptions(this);
        assert null != options;
    }
}
