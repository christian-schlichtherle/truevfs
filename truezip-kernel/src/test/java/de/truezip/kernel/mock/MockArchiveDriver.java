/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.mock;

import de.truezip.kernel.*;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.Maps;
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
                Maps.initialCapacity(config.getNumEntries()));
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
    public IOPool<?> getIOPool() {
        return config.getIOPoolProvider().getIOPool();
    }

    @Override
    protected InputService<MockArchiveDriverEntry> newInputService(
            final FsModel model,
            final InputSocket<?> input)
    throws IOException {
        final FsMountPoint mp = model.getMountPoint();
        input.localTarget(); // don't care for the result
        final MockArchive
                c = containers.get(mp);
        if (null == c)
            throw new NoSuchFileException(mp.toString());
        return c.newInputService();
    }

    @Override
    protected OutputService<MockArchiveDriverEntry> newOutputService(
            final FsModel model,
            final OutputSocket<?> output,
            final @CheckForNull @WillNotClose InputService<MockArchiveDriverEntry> input)
    throws IOException {
        final FsMountPoint mp = model.getMountPoint();
        output.localTarget(); // don't care for the result
        final MockArchive
                n = MockArchive.create(config);
        MockArchive o = containers.get(mp);
        if (null == o)
            o = containers.putIfAbsent(mp, n);
        return (null != o ? o : n).newOutputService();
    }

    @Override
    public MockArchiveDriverEntry newEntry(
            String name,
            Type type,
            BitField<FsAccessOption> mknod,
            @CheckForNull Entry template) {
        return new MockArchiveDriverEntry(normalize(name, type), type, template);
    }
}
