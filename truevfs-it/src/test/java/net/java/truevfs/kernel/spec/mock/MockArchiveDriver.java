/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.mock;

import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.Entry.Type;
import net.java.truecommons.cio.InputService;
import net.java.truecommons.cio.IoBufferPool;
import net.java.truecommons.cio.OutputService;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.HashMaps;
import net.java.truevfs.kernel.spec.*;
import net.java.truevfs.kernel.spec.cio.MultiplexingOutputService;

import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class MockArchiveDriver extends FsArchiveDriver<MockArchiveDriverEntry> {

    private final FsTestConfig config;
    private final ConcurrentMap<FsMountPoint, MockArchive> containers;

    public MockArchiveDriver() {
        this.config = FsTestConfig.get();
        this.containers = new ConcurrentHashMap<>(HashMaps.initialCapacity(config.getNumEntries()));
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link StandardCharsets#UTF_8}.
     */
    @Override
    public Charset getCharset() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public IoBufferPool getPool() {
        return config.getPool();
    }

    @Override
    protected InputService<MockArchiveDriverEntry> newInput(
            final FsModel model,
            final FsInputSocketSource input)
            throws IOException {
        final FsMountPoint mp = model.getMountPoint();
        final MockArchive c = containers.get(mp);
        if (null == c) {
            throw new NoSuchFileException(mp.toString());
        }
        return c.newInputService();
    }

    @Override
    protected OutputService<MockArchiveDriverEntry> newOutput(
            final FsModel model,
            final FsOutputSocketSink sink,
            final @CheckForNull @WillNotClose InputService<MockArchiveDriverEntry> input) {
        final FsMountPoint mp = model.getMountPoint();
        final MockArchive n = MockArchive.create(config);
        MockArchive o = containers.get(mp);
        if (null == o)
            o = containers.putIfAbsent(mp, n);
        return new MultiplexingOutputService<>(getPool(),
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
