/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.mock;

import net.truevfs.kernel.*;
import net.truevfs.kernel.cio.Entry.Type;
import net.truevfs.kernel.cio.*;
import net.truevfs.kernel.io.Sink;
import net.truevfs.kernel.io.Source;
import net.truevfs.kernel.util.BitField;
import net.truevfs.kernel.util.HashMaps;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class MockArchiveDriver extends FsArchiveDriver<MockArchiveDriverEntry> {

    public static final Charset MOCK_CHARSET = Charset.forName("UTF-8");

    private final TestConfig config;
    private final ConcurrentMap<FsMountPoint, MockArchive>
            containers;

    public MockArchiveDriver() {
        this(null);
    }

    public MockArchiveDriver(@CheckForNull TestConfig config) {
        if (null == config)
            config = TestConfig.get();
        this.config = config;
        this.containers = new ConcurrentHashMap<>(
                HashMaps.initialCapacity(config.getNumEntries()));
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link #MOCK_CHARSET}.
     */
    @Override
    public Charset getCharset() {
        return MOCK_CHARSET;
    }

    @Override
    public IoPool<?> getIoPool() {
        return config.getIoPoolProvider().getIoPool();
    }

    @Override
    protected InputService<MockArchiveDriverEntry> newInput(
            final FsModel model,
            final FsInputSocketSource input)
    throws IOException {
        final FsMountPoint mp = model.getMountPoint();
        final MockArchive c = containers.get(mp);
        if (null == c)
            throw new NoSuchFileException(mp.toString());
        return c.newInputService();
    }

    @Override
    protected OutputService<MockArchiveDriverEntry> newOutput(
            final FsModel model,
            final FsOutputSocketSink sink,
            final @CheckForNull @WillNotClose InputService<MockArchiveDriverEntry> input)
    throws IOException {
        final FsMountPoint mp = model.getMountPoint();
        final MockArchive n = MockArchive.create(config);
        MockArchive o = containers.get(mp);
        if (null == o)
            o = containers.putIfAbsent(mp, n);
        return new MultiplexingOutputService<>(getIoPool(),
                (null != o ? o : n).newOutputService());
    }

    @Override
    public MockArchiveDriverEntry newEntry(
            BitField<FsAccessOption> options,
            String name,
            Type type,
            @CheckForNull Entry template) {
        return new MockArchiveDriverEntry(normalize(name, type), type, template);
    }
}
