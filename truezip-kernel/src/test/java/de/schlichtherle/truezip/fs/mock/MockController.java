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
import de.schlichtherle.truezip.io.ThrowingInputStream;
import de.schlichtherle.truezip.io.ThrowingOutputStream;
import de.schlichtherle.truezip.io.ThrowingSeekableByteChannel;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.ThrowingReadOnlyFile;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.test.TestConfig;
import de.schlichtherle.truezip.test.ThrowControl;
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
    private final TestConfig config;
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    private final ConcurrentMap<FsEntryName, IOEntry<?>>
            map = new ConcurrentHashMap<FsEntryName, IOEntry<?>>();
    private volatile @CheckForNull ThrowControl control;

    /**
     * Constructs a new mock controller.
     * 
     * @param model The file system model.
     * @param parent The parent file system controller.
     * @param config The mocking configuration.
     */
    public MockController(  final M model,
                            final @CheckForNull FsController<?> parent,
                            final @CheckForNull TestConfig config) {
        assert null == model.getParent()
                ? null == parent
                : model.getParent().equals(parent.getModel());
        this.model = model;
        this.parent = parent;
        this.config = null != config ? config : TestConfig.get();
    }

    private ThrowControl getControl() {
        final ThrowControl control = this.control;
        return null != control ? control : (this.control = config.getThrowControl());
    }

    private void checkAnyException() throws IOException {
        getControl().check(this, IOException.class);
        checkUndeclaredException();
    }

    private void checkUndeclaredException() {
        getControl().check(this, RuntimeException.class);
        getControl().check(this, Error.class);
    }

    @Override
    public M getModel() {
        checkUndeclaredException();
        return model;
    }

    @Override
    public FsController<?> getParent() {
        checkUndeclaredException();
        return parent;
    }

    @Override
    @Deprecated
    public Icon getOpenIcon() throws IOException {
        checkAnyException();
        return null;
    }

    @Override
    @Deprecated
    public Icon getClosedIcon() throws IOException {
        checkAnyException();
        return null;
    }

    @Override
    public boolean isReadOnly() throws IOException {
        checkAnyException();
        return false;
    }

    @Override
    public FsEntry getEntry(FsEntryName name) throws IOException {
        checkAnyException();
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        checkAnyException();
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        checkAnyException();
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        checkAnyException();
        assert null != name;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<FsOutputOption> options)
    throws IOException {
        checkAnyException();
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
        checkAnyException();
        assert null != name;
        assert null != types;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsInputOption> options) {
        checkUndeclaredException();
        assert null != name;
        assert null != options;

        class Input extends DelegatingInputSocket<Entry> {
            @Override
            protected InputSocket<? extends Entry> getDelegate()
            throws IOException {
                checkAnyException();
                final IOEntry<?> buffer = map.get(name);
                if (null == buffer)
                    throw new FileNotFoundException(name.toString());
                return buffer.getInputSocket();
            }

            @Override
            public ReadOnlyFile newReadOnlyFile()
            throws IOException {
                return new ThrowingReadOnlyFile(
                        getBoundSocket().newReadOnlyFile(),
                        config.getThrowControl());
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel()
            throws IOException {
                return new ThrowingSeekableByteChannel(
                        getBoundSocket().newSeekableByteChannel(),
                        config.getThrowControl());
            }

            @Override
            public InputStream newInputStream()
            throws IOException {
                return new ThrowingInputStream(
                        getBoundSocket().newInputStream(),
                        config.getThrowControl());
            }
        } // Input

        return new Input();
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsOutputOption> options,
            final Entry template) {
        checkUndeclaredException();
        assert null != name;
        assert null != options;

        class Output extends DelegatingOutputSocket<Entry> {
            @Override
            protected OutputSocket<? extends Entry> getDelegate()
            throws IOException {
                checkAnyException();
                final IOEntry<?> n = new ByteArrayIOBuffer(
                        name.toString(), config.getDataSize());
                final IOEntry<?> o = map.putIfAbsent(name, n);
                return (null != o ? o : n).getOutputSocket();
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel()
            throws IOException {
                return new ThrowingSeekableByteChannel(
                        getBoundSocket().newSeekableByteChannel(),
                        config.getThrowControl());
            }

            @Override
            public OutputStream newOutputStream()
            throws IOException {
                return new ThrowingOutputStream(
                        getBoundSocket().newOutputStream(),
                        config.getThrowControl());
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
        checkAnyException();
        assert null != name;
        assert null != type;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        checkAnyException();
        assert null != name;
        assert null != options;
        throw new UnsupportedOperationException();
    }

    @Override
    public <X extends IOException> void
    sync(   BitField<FsSyncOption> options,
            ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        checkAnyException();
        assert null != options;
        assert null != handler;
    }
}
