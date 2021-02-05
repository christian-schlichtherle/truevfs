/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.mock;

import global.namespace.truevfs.commons.cio.*;
import global.namespace.truevfs.commons.cio.Entry.Access;
import global.namespace.truevfs.commons.cio.Entry.Size;
import global.namespace.truevfs.commons.io.DecoratingOutputStream;
import global.namespace.truevfs.commons.shed.HashMaps;
import global.namespace.truevfs.kernel.api.FsTestConfig;
import global.namespace.truevfs.kernel.api.FsThrowManager;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.NoSuchFileException;
import java.util.*;

import static global.namespace.truevfs.commons.cio.Entry.ALL_ACCESS;
import static global.namespace.truevfs.commons.cio.Entry.ALL_SIZES;

/**
 * @author Christian Schlichtherle
 */
public class MockArchive implements Container<MockArchiveDriverEntry> {

    final Map<String, MockArchiveDriverEntry> entries;
    private final FsTestConfig config;
    private @CheckForNull
    FsThrowManager control;

    public static MockArchive create(@CheckForNull FsTestConfig config) {
        if (null == config) {
            config = FsTestConfig.get();
        }
        return new MockArchive(new LinkedHashMap<>(HashMaps.initialCapacity(config.getNumEntries())), config);
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
    public Collection<MockArchiveDriverEntry> entries() {
        checkUndeclaredExceptions();
        return Collections.unmodifiableCollection(entries.values());
    }

    @Override
    public Optional<MockArchiveDriverEntry> entry(final String name) {
        checkUndeclaredExceptions();
        return Optional.ofNullable(entries.get(name));
    }

    public InputContainer<MockArchiveDriverEntry> newInputService() {
        checkUndeclaredExceptions();
        return new ThrowingInputContainer<>(new MockInputContainer(entries, config), config);
    }

    public OutputContainer<MockArchiveDriverEntry> newOutputService() {
        checkUndeclaredExceptions();
        return new ThrowingOutputContainer<>(new MockOutputContainer(entries, config), config);
    }

    @Override
    public void close() throws IOException {
        getThrowControl().trigger(new IllegalStateException());
    }

    private static final class MockInputContainer extends MockArchive implements InputContainer<MockArchiveDriverEntry> {

        MockInputContainer(Map<String, MockArchiveDriverEntry> entries, FsTestConfig config) {
            super(entries, config);
        }

        @Override
        public InputSocket<MockArchiveDriverEntry> input(final String name) {
            Objects.requireNonNull(name);

            return new InputSocket<MockArchiveDriverEntry>() {

                @Override
                public MockArchiveDriverEntry getTarget() throws IOException {
                    final MockArchiveDriverEntry entry = entries.get(name);
                    if (null == entry) {
                        throw new NoSuchFileException(name, null, "Entry not found!");
                    }
                    return entry;
                }

                @Override
                public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                    return socket().stream(peer);
                }

                @Override
                public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer)
                        throws IOException {
                    return socket().channel(peer);
                }

                InputSocket<? extends IoBuffer> socket() throws IOException {
                    return getTarget().getBuffer(getPool()).input();
                }
            };
        }

        @Override
        public void close() {
        }
    }

    private static final class MockOutputContainer extends MockArchive implements OutputContainer<MockArchiveDriverEntry> {

        boolean busy;

        MockOutputContainer(Map<String, MockArchiveDriverEntry> entries, FsTestConfig config) {
            super(entries, config);
        }

        @Override
        public OutputSocket<MockArchiveDriverEntry> output(final MockArchiveDriverEntry entry) {
            Objects.requireNonNull(entry);

            return new OutputSocket<MockArchiveDriverEntry>() {

                @Override
                public MockArchiveDriverEntry getTarget() {
                    return entry;
                }

                @Override
                public OutputStream stream(final Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
                    return new DecoratingOutputStream() {

                        boolean closed;

                        {
                            if (busy) {
                                throw new IOException("Busy!");
                            }
                            this.out = socket().stream(peer);
                            busy = true;
                        }

                        @Override
                        public void close() throws IOException {
                            if (!closed) {
                                out.close();
                                copyProperties();
                                closed = true;
                                busy = false;
                            }
                        }
                    };
                }

                OutputSocket<? extends IoBuffer> socket() throws IOException {
                    entries.put(entry.getName(), entry);
                    return getTarget().getBuffer(getPool()).output();
                }

                void copyProperties() {
                    final MockArchiveDriverEntry target = getTarget();
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
            };
        }

        @Override
        public void close() {
        }
    }
}
