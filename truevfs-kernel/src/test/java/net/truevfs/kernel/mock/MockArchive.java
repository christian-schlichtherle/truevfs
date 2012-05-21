/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.mock;

import net.truevfs.kernel.TestConfig;
import net.truevfs.kernel.ThrowManager;
import static net.truevfs.kernel.cio.Entry.ALL_ACCESS;
import static net.truevfs.kernel.cio.Entry.ALL_SIZES;
import net.truevfs.kernel.cio.Entry.Access;
import net.truevfs.kernel.cio.Entry.Size;
import net.truevfs.kernel.cio.*;
import net.truevfs.kernel.io.DecoratingOutputStream;
import net.truevfs.kernel.util.HashMaps;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.NoSuchFileException;
import java.util.*;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class MockArchive
implements Container<MockArchiveDriverEntry> {

    final Map<String, MockArchiveDriverEntry> entries;
    private final TestConfig config;
    private @CheckForNull ThrowManager control;

    public static MockArchive create(@CheckForNull TestConfig config) {
        if (null == config)
            config = TestConfig.get();
        return new MockArchive(
                new LinkedHashMap<String, MockArchiveDriverEntry>(
                    HashMaps.initialCapacity(config.getNumEntries())),
                config);
    }

    private MockArchive(
            final Map<String, MockArchiveDriverEntry> entries,
            final TestConfig config) {
        this.entries = entries;
        this.config = config;
    }

    private ThrowManager getThrowControl() {
        final ThrowManager control = this.control;
        return null != control ? control : (this.control = config.getThrowControl());
    }

    private void checkUndeclaredExceptions() {
        getThrowControl().check(this, RuntimeException.class);
        getThrowControl().check(this, Error.class);
    }

    public IoPoolProvider getIOPoolProvider() {
        return config.getIoPoolProvider();
    }

    public final IoPool<?> getIOPool() {
        return getIOPoolProvider().getIoPool();
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
                new DisconnectingInputService<>(
                    new MockInputService(entries, config)),
                config);
    }

    public OutputService<MockArchiveDriverEntry> newOutputService() {
        checkUndeclaredExceptions();
        return new ThrowingOutputService<>(
                new DisconnectingOutputService<>(
                    new MockOutputService(entries, config)),
                config);
    }

    private static final class MockInputService
    extends MockArchive
    implements InputService<MockArchiveDriverEntry> {

        MockInputService(  Map<String, MockArchiveDriverEntry> entries,
                        TestConfig config) {
            super(entries, config);
        }

        @Override
        public InputSocket<MockArchiveDriverEntry> input(final String name) {
            Objects.requireNonNull(name);

            final class Input extends AbstractInputSocket<MockArchiveDriverEntry> {
                @Override
                public MockArchiveDriverEntry localTarget()
                throws IOException {
                    final MockArchiveDriverEntry entry = entries.get(name);
                    if (null == entry)
                        throw new NoSuchFileException(name, null, "Entry not found!");
                    return entry;
                }

                @Override
                public InputStream stream() throws IOException {
                    return getBufferInputSocket().stream();
                }

                @Override
                public SeekableByteChannel channel()
                throws IOException {
                    return getBufferInputSocket().channel();
                }

                InputSocket<? extends IoEntry<?>>
                getBufferInputSocket() throws IOException {
                    return localTarget()
                            .getBuffer(getIOPool())
                            .input();
                }
            } // Input

            return new Input();
        }

        @Override
        @DischargesObligation
        public void close() { }
    } // MockInputService

    private static final class MockOutputService
    extends MockArchive
    implements OutputService<MockArchiveDriverEntry> {
        boolean busy;

        MockOutputService( Map<String, MockArchiveDriverEntry> entries,
                        TestConfig config) {
            super(entries, config);
        }

        @Override
        public OutputSocket<MockArchiveDriverEntry> output(
                final MockArchiveDriverEntry entry) {
            Objects.requireNonNull(entry);

            final class Output extends AbstractOutputSocket<MockArchiveDriverEntry> {
                @Override
                public MockArchiveDriverEntry localTarget() {
                    return entry;
                }

                @Override
                public OutputStream stream() throws IOException {
                    final class Stream extends DecoratingOutputStream {
                        boolean closed;

                        Stream() throws IOException {
                            if (busy)
                                throw new IOException("Busy!");
                            this.out = getBufferOutputSocket().stream();
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
                    return getBufferOutputSocket().channel();
                }*/

                OutputSocket<? extends IoEntry<?>>
                getBufferOutputSocket() throws IOException {
                    entries.put(entry.getName(), entry);
                    return localTarget()
                            .getBuffer(getIOPool())
                            .output();
                }

                void copyProperties() {
                    final MockArchiveDriverEntry target = localTarget();
                    final IoBuffer<?> buffer;
                    try {
                        buffer = target.getBuffer(getIOPool());
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
        public void close() { }
    } // MockOutputService
}
