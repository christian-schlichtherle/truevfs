/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs.mock;

import de.truezip.kernel.TestConfig;
import de.truezip.kernel.ThrowControl;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.fs.FsController;
import de.truezip.kernel.fs.FsEntry;
import de.truezip.kernel.fs.FsModel;
import de.truezip.kernel.fs.FsSyncException;
import de.truezip.kernel.fs.addr.FsEntryName;
import de.truezip.kernel.fs.option.FsAccessOption;
import de.truezip.kernel.fs.option.FsSyncOption;
import de.truezip.kernel.io.ThrowingInputStream;
import de.truezip.kernel.io.ThrowingOutputStream;
import de.truezip.kernel.io.ThrowingSeekableByteChannel;
import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.rof.ThrowingReadOnlyFile;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
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
public class MockController extends FsController<FsModel> {

    private final @Nullable FsController<?> parent;
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    private final ConcurrentMap<FsEntryName, IOEntry<?>>
            map = new ConcurrentHashMap<FsEntryName, IOEntry<?>>();
    private final TestConfig config;
    private volatile @CheckForNull ThrowControl control;

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

    private ThrowControl getThrowControl() {
        final ThrowControl control = this.control;
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
    public FsEntry getEntry(FsEntryName name) throws IOException {
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
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsAccessOption> options) {
        checkUndeclaredExceptions(this);
        assert null != name;
        assert null != options;

        class Input extends DelegatingInputSocket<Entry> {
            @Override
            protected InputSocket<? extends Entry> getDelegate()
            throws IOException {
                checkAllExceptions(this);
                final IOEntry<?> buffer = map.get(name);
                if (null == buffer)
                    throw new FileNotFoundException(name.toString());
                return buffer.getInputSocket();
            }

            @Override
            public ReadOnlyFile newReadOnlyFile()
            throws IOException {
                return new ThrowingReadOnlyFile(
                        getBoundDelegate().newReadOnlyFile(),
                        config.getThrowControl());
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel()
            throws IOException {
                return new ThrowingSeekableByteChannel(
                        getBoundDelegate().newSeekableByteChannel(),
                        config.getThrowControl());
            }

            @Override
            public InputStream newInputStream()
            throws IOException {
                return new ThrowingInputStream(
                        getBoundDelegate().newInputStream(),
                        config.getThrowControl());
            }
        } // Input

        return new Input();
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsAccessOption> options,
            final Entry template) {
        checkUndeclaredExceptions(this);
        assert null != name;
        assert null != options;

        class Output extends DelegatingOutputSocket<Entry> {
            @Override
            protected OutputSocket<? extends Entry> getDelegate()
            throws IOException {
                checkAllExceptions(this);
                final IOEntry<?> n = new ByteArrayIOBuffer(
                        name.toString(), config.getDataSize());
                IOEntry<?> o = map.get(name);
                if (null == o)
                    o = map.putIfAbsent(name, n);
                return (null != o ? o : n).getOutputSocket();
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel()
            throws IOException {
                return new ThrowingSeekableByteChannel(
                        getBoundDelegate().newSeekableByteChannel(),
                        config.getThrowControl());
            }

            @Override
            public OutputStream newOutputStream()
            throws IOException {
                return new ThrowingOutputStream(
                        getBoundDelegate().newOutputStream(),
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
    public <X extends IOException> void
    sync(   BitField<FsSyncOption> options,
            ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        checkAllExceptions(this);
        assert null != options;
        assert null != handler;
    }
}