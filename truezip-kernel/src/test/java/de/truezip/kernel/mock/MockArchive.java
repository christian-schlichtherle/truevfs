/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.mock;

import de.truezip.kernel.TestConfig;
import de.truezip.kernel.ThrowManager;
import static de.truezip.kernel.cio.Entry.ALL_ACCESS_SET;
import static de.truezip.kernel.cio.Entry.ALL_SIZE_SET;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Size;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.util.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.NoSuchFileException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
public class MockArchive
implements Container<MockArchiveDriverEntry> {

    final Map<String, MockArchiveDriverEntry> entries;
    private final TestConfig config;
    private @CheckForNull ThrowManager control;

    public static MockArchive create(
            @CheckForNull TestConfig config) {
        if (null == config)
            config = TestConfig.get();
        return new MockArchive(
                new LinkedHashMap<String, MockArchiveDriverEntry>(
                    Maps.initialCapacity(config.getNumEntries())),
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

    private IOPoolProvider getIOPoolProvider() {
        return config.getIOPoolProvider();
    }

    final IOPool<?> getIOPool() {
        return getIOPoolProvider().getIOPool();
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
    public MockArchiveDriverEntry getEntry(String name) {
        checkUndeclaredExceptions();
        return entries.get(name);
    }

    public InputService<MockArchiveDriverEntry> newInputService() {
        checkUndeclaredExceptions();
        return new ThrowingInputService<MockArchiveDriverEntry>(
                new DisconnectingInputService<MockArchiveDriverEntry>(
                    new MockInputService(entries, config)),
                config);
    }

    public OutputService<MockArchiveDriverEntry> newOutputService() {
        checkUndeclaredExceptions();
        return new ThrowingOutputService<MockArchiveDriverEntry>(
                new DisconnectingOutputService<MockArchiveDriverEntry>(
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
        public InputSocket<MockArchiveDriverEntry> getInputSocket(
                final String name) {
            if (null == name)
                throw new NullPointerException();

            class Input extends InputSocket<MockArchiveDriverEntry> {
                @Override
                public MockArchiveDriverEntry getLocalTarget()
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

                InputSocket<? extends IOEntry<?>>
                getBufferInputSocket() throws IOException {
                    return getLocalTarget()
                            .getBuffer(getIOPool())
                            .getInputSocket();
                }
            } // Input

            return new Input();
        }

        @Override
        public void close() { }
    } // MockInputService

    private static final class MockOutputService
    extends MockArchive
    implements OutputService<MockArchiveDriverEntry> {

        MockOutputService( Map<String, MockArchiveDriverEntry> entries,
                        TestConfig config) {
            super(entries, config);
        }

        @Override
        public OutputSocket<MockArchiveDriverEntry> getOutputSocket(
                final MockArchiveDriverEntry entry) {
            if (null == entry)
                throw new NullPointerException();

            class Output extends OutputSocket<MockArchiveDriverEntry> {
                @Override
                public MockArchiveDriverEntry getLocalTarget() {
                    return entry;
                }

                @Override
                public SeekableByteChannel channel()
                throws IOException {
                    return getBufferOutputSocket().channel();
                }

                @Override
                public OutputStream stream()
                throws IOException {
                    class MockOutputStream extends DecoratingOutputStream {
                        MockOutputStream() throws IOException {
                            super(getBufferOutputSocket().stream());
                        }

                        @Override
                        public void close() throws IOException {
                            out.close();
                            copyProperties();
                        }
                    } // MockOutputStream

                    return new MockOutputStream();
                }

                OutputSocket<? extends IOEntry<?>>
                getBufferOutputSocket() throws IOException {
                    entries.put(entry.getName(), entry);
                    return getLocalTarget()
                            .getBuffer(getIOPool())
                            .getOutputSocket();
                }

                void copyProperties() {
                    final MockArchiveDriverEntry dst = getLocalTarget();
                    final IOBuffer<?> src;
                    try {
                        src = dst.getBuffer(getIOPool());
                    } catch (IOException ex) {
                        throw new AssertionError(ex);
                    }
                    for (final Size type : ALL_SIZE_SET)
                        dst.setSize(type, src.getSize(type));
                    for (final Access type : ALL_ACCESS_SET)
                        dst.setTime(type, src.getTime(type));
                }
            } // Output

            return new Output();
        }

        @Override
        public void close() { }
    } // MockOutputService
}