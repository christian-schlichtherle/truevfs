/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.mock;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.io.mock.MockInputStream;
import de.schlichtherle.truezip.io.mock.MockOutputStream;
import de.schlichtherle.truezip.io.mock.MockSeekableByteChannel;
import static de.schlichtherle.truezip.mock.MockControl.check;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.mock.MockReadOnlyFile;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
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
import javax.swing.Icon;

/**
 * @param   <M> The file system model.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class MockController<M extends FsModel> extends FsController<M> {

    private final M model;
    private final @Nullable FsController<?> parent;
    private final int initialCapacity;
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    private final ConcurrentMap<FsEntryName, IOEntry<?>>
            map = new ConcurrentHashMap<FsEntryName, IOEntry<?>>();

    /**
     * Equivalent to {@link #MockController(FsModel, FsController, int)
     * new MockController(model, parent, 1024)}.
     * 
     * @param model the file system model.
     * @param parent the parent file system controller.
     */
    public MockController(  final M model,
                            final @CheckForNull FsController<?> parent) {
        this(model, parent, 1024);
    }

    /**
     * Constructs a new mock controller.
     * 
     * @param model the file system model.
     * @param parent the parent file system controller.
     * @param initialCapacity the initial capacity of the byte array of any
     *        I/O buffer.
     */
    public MockController(  final M model,
                            final @CheckForNull FsController<?> parent,
                            final int initialCapacity) {
        assert null == model.getParent()
                ? null == parent
                : model.getParent().equals(parent.getModel());
        this.model = model;
        this.parent = parent;
        this.initialCapacity = initialCapacity;
    }

    @Override
    public M getModel() {
        check(this, RuntimeException.class);
        return model;
    }

    @Override
    public FsController<?> getParent() {
        check(this, RuntimeException.class);
        return parent;
    }

    @Override
    @Deprecated
    public Icon getOpenIcon() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        return null;
    }

    @Override
    @Deprecated
    public Icon getClosedIcon() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        return null;
    }

    @Override
    public boolean isReadOnly() throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        return false;
    }

    @Override
    public FsEntry getEntry(FsEntryName name) throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<FsOutputOption> options)
    throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
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
            BitField<FsOutputOption> options)
    throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        assert null != name;
        assert null != types;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsInputOption> options) {
        check(this, RuntimeException.class);
        assert null != name;
        assert null != options;

        class Input extends DelegatingInputSocket<Entry> {
            @Override
            protected InputSocket<? extends Entry> getDelegate()
            throws IOException {
                check(this, IOException.class);
                check(this, RuntimeException.class);
                final IOEntry<?> buffer = map.get(name);
                if (null == buffer)
                    throw new FileNotFoundException(name.toString());
                return buffer.getInputSocket();
            }

            @Override
            public ReadOnlyFile newReadOnlyFile()
            throws IOException {
                return new MockReadOnlyFile(
                        getBoundSocket().newReadOnlyFile());
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel()
            throws IOException {
                return new MockSeekableByteChannel(
                        getBoundSocket().newSeekableByteChannel());
            }

            @Override
            public InputStream newInputStream()
            throws IOException {
                return new MockInputStream(
                        getBoundSocket().newInputStream());
            }
        } // Input

        return new Input();
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsOutputOption> options,
            final Entry template) {
        check(this, RuntimeException.class);
        assert null != name;
        assert null != options;

        class Output extends DelegatingOutputSocket<Entry> {
            @Override
            protected OutputSocket<? extends Entry> getDelegate()
            throws IOException {
                check(this, IOException.class);
                check(this, RuntimeException.class);
                final IOEntry<?> n = new ByteArrayIOBuffer(
                        name.toString(), initialCapacity);
                final IOEntry<?> o = map.putIfAbsent(name, n);
                return (null != o ? o : n).getOutputSocket();
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel()
            throws IOException {
                return new MockSeekableByteChannel(
                        getBoundSocket().newSeekableByteChannel());
            }

            @Override
            public OutputStream newOutputStream()
            throws IOException {
                return new MockOutputStream(
                        getBoundSocket().newOutputStream());
            }
        } // Output

        return new Output();
    }

    @Override
    public void mknod(  FsEntryName name,
                        Type type,
                        BitField<FsOutputOption> options,
                        Entry template)
    throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        assert null != name;
        assert null != type;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        assert null != name;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public <X extends IOException> void
    sync(   BitField<FsSyncOption> options,
            ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        check(this, IOException.class);
        check(this, RuntimeException.class);
        assert null != options;
        assert null != handler;
    }
}
