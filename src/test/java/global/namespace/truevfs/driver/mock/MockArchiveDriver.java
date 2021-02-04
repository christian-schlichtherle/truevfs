/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.mock;

import global.namespace.truevfs.comp.cio.*;
import global.namespace.truevfs.comp.cio.Entry.Type;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.comp.shed.HashMaps;
import global.namespace.truevfs.kernel.api.*;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Christian Schlichtherle
 */
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
    protected InputContainer<MockArchiveDriverEntry> newInput(
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
    protected OutputContainer<MockArchiveDriverEntry> newOutput(
            final FsModel model,
            final FsOutputSocketSink sink,
            final @CheckForNull InputContainer<MockArchiveDriverEntry> input) {
        final FsMountPoint mp = model.getMountPoint();
        final MockArchive n = MockArchive.create(config);
        MockArchive o = containers.get(mp);
        if (null == o)
            o = containers.putIfAbsent(mp, n);
        return new MultiplexingOutputContainer<>(getPool(),
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
