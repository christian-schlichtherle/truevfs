/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.mock;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import net.java.truecommons.cio.*;
import net.java.truecommons.cio.Entry.Access;
import net.java.truecommons.cio.Entry.Size;
import net.java.truecommons.io.DecoratingOutputStream;
import net.java.truecommons.shed.HashMaps;
import net.java.truevfs.kernel.spec.FsTestConfig;
import net.java.truevfs.kernel.spec.FsThrowManager;
import net.java.truevfs.kernel.spec.cio.ThrowingInputService;
import net.java.truevfs.kernel.spec.cio.ThrowingOutputService;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.NoSuchFileException;
import java.util.*;

import static net.java.truecommons.cio.Entry.ALL_ACCESS;
import static net.java.truecommons.cio.Entry.ALL_SIZES;

/**
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class MockArchive implements Container<MockArchiveDriverEntry> {

    final Map<String, MockArchiveDriverEntry> entries;
    private final FsTestConfig config;
    private @CheckForNull
    FsThrowManager control;

    public static MockArchive create(@CheckForNull FsTestConfig config) {
        if (null == config) config = FsTestConfig.get();
        return new MockArchive(
                new LinkedHashMap<String, MockArchiveDriverEntry>(
                        HashMaps.initialCapacity(config.getNumEntries())),
                config);
    }

    private MockArchive(
            final Map<String, MockArchiveDriverEntry> entries,
            final FsTestConfig config) {
        this.entries = entries;
        this.config = config;
    }

    private FsThrowManager getThrowControl() {
        final FsThrowManager control = this.control;
        return null != control ? control : (this.control = config.getThrowControl());
    }

    private void checkUndeclaredExceptions() {
        getThrowControl().check(this, RuntimeException.class);
        getThrowControl().check(this, Error.class);
    }

    public IoBufferPool getPool() {
        checkUndeclaredExceptions();
        return config.getPool();
    }

    @Override
    public int size() {
        checkUndeclaredExceptions();
        return entries.size();
    }

    @Override
    public Iterator<MockArchiveDriverEntry> iterator() {
        checkUndeclaredExceptions();
        return Collections.unmodifiableCollection(entries.values()).iterator();
    }

    @Override
    public MockArchiveDriverEntry entry(String name) {
        checkUndeclaredExceptions();
        return entries.get(name);
    }

    public InputService<MockArchiveDriverEntry> newInputService() {
        checkUndeclaredExceptions();
        return new ThrowingInputService<>(
                new MockInputService(entries, config),
                config);
    }

    public OutputService<MockArchiveDriverEntry> newOutputService() {
        checkUndeclaredExceptions();
        return new ThrowingOutputService<>(
                new MockOutputService(entries, config),
                config);
    }

    @Override
    public void close() throws IOException {
        getThrowControl().trigger(new IllegalStateException());
    }

    private static final class MockInputService
            extends MockArchive implements InputService<MockArchiveDriverEntry> {

        MockInputService(
                Map<String, MockArchiveDriverEntry> entries,
                FsTestConfig config) {
            super(entries, config);
        }

        @Override
        public InputSocket<MockArchiveDriverEntry> input(final String name) {
            Objects.requireNonNull(name);

            final class Input
                    extends AbstractInputSocket<MockArchiveDriverEntry> {
                @Override
                public MockArchiveDriverEntry target()
                        throws IOException {
                    final MockArchiveDriverEntry entry = entries.get(name);
                    if (null == entry)
                        throw new NoSuchFileException(name, null, "Entry not found!");
                    return entry;
                }

                @Override
                public InputStream stream(OutputSocket<? extends Entry> peer)
                        throws IOException {
                    return socket().stream(peer);
                }

                @Override
                public SeekableByteChannel channel(OutputSocket<? extends Entry> peer)
                        throws IOException {
                    return socket().channel(peer);
                }

                InputSocket<? extends IoBuffer> socket() throws IOException {
                    return target().getBuffer(getPool()).input();
                }
            } // Input

            return new Input();
        }

        @Override
        @DischargesObligation
        public void close() {
        }
    } // MockInputService

    private static final class MockOutputService
            extends MockArchive implements OutputService<MockArchiveDriverEntry> {
        boolean busy;

        MockOutputService(
                Map<String, MockArchiveDriverEntry> entries,
                FsTestConfig config) {
            super(entries, config);
        }

        @Override
        public OutputSocket<MockArchiveDriverEntry> output(
                final MockArchiveDriverEntry entry) {
            Objects.requireNonNull(entry);

            final class Output
                    extends AbstractOutputSocket<MockArchiveDriverEntry> {
                @Override
                public MockArchiveDriverEntry target() {
                    return entry;
                }

                @Override
                public OutputStream stream(final InputSocket<? extends Entry> peer)
                        throws IOException {
                    final class Stream extends DecoratingOutputStream {
                        boolean closed;

                        Stream() throws IOException {
                            if (busy) throw new IOException("Busy!");
                            this.out = socket().stream(peer);
                            busy = true;
                        }

                        @Override
                        public void close() throws IOException {
                            if (closed)
                                return;
                            out.close();
                            copyProperties();
                            closed = true;
                            busy = false;
                        }
                    } // Stream

                    return new Stream();
                }

                /*@Override
                public SeekableByteChannel channel() throws IOException {
                    return socket().channel();
                }*/

                OutputSocket<? extends IoBuffer> socket() throws IOException {
                    entries.put(entry.getName(), entry);
                    return target().getBuffer(getPool()).output();
                }

                void copyProperties() {
                    final MockArchiveDriverEntry target = target();
                    final IoBuffer buffer;
                    try {
                        buffer = target.getBuffer(getPool());
                    } catch (final IOException ex) {
                        throw new AssertionError(ex);
                    }
                    for (final Size type : ALL_SIZES)
                        target.setSize(type, buffer.getSize(type));
                    for (final Access type : ALL_ACCESS)
                        target.setTime(type, buffer.getTime(type));
                }
            } // Output

            return new Output();
        }

        @Override
        @DischargesObligation
        public void close() {
        }
    } // MockOutputService
}
