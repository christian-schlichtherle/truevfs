/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.mock;

import de.schlichtherle.truezip.cio.Entry.Type;
import de.schlichtherle.truezip.cio.*;
import de.schlichtherle.truezip.fs.FsCharsetArchiveDriver;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.addr.FsMountPoint;
import de.schlichtherle.truezip.fs.option.FsOutputOption;
import de.schlichtherle.truezip.test.TestConfig;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.Maps;
import java.io.CharConversionException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class MockArchiveDriver
extends FsCharsetArchiveDriver<MockArchiveDriverEntry> {

    private static final Charset charset = Charset.forName("UTF-8");
    
    private final TestConfig config;
    private final ConcurrentMap<FsMountPoint, MockArchive>
            containers;

    public MockArchiveDriver() {
        this(null);
    }

    public MockArchiveDriver(@CheckForNull TestConfig config) {
        super(charset);
        if (null == config)
            config = TestConfig.get();
        this.config = config;
        this.containers = new ConcurrentHashMap<FsMountPoint, MockArchive>(
                Maps.initialCapacity(config.getNumEntries()));
    }

    private IOPoolProvider getIOPoolProvider() {
        return config.getIOPoolProvider();
    }

    @Override
    protected final IOPool<?> getIOPool() {
        return getIOPoolProvider().get();
    }

    @Override
    public InputService<MockArchiveDriverEntry> newInputService(
            final FsModel model,
            final InputSocket<?> input)
    throws IOException {
        final FsMountPoint mp = model.getMountPoint();
        input.getLocalTarget(); // don't care for the result
        final MockArchive
                c = containers.get(mp);
        if (null == c)
            throw new FileNotFoundException(mp.toString());
        return c.newInputService();
    }

    @Override
    public OutputService<MockArchiveDriverEntry> newOutputService(
            FsModel model,
            OutputSocket<?> output,
            InputService<MockArchiveDriverEntry> source)
    throws IOException {
        final FsMountPoint mp = model.getMountPoint();
        output.getLocalTarget(); // don't care for the result
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
            Entry template,
            BitField<FsOutputOption> mknod)
    throws CharConversionException {
        return new MockArchiveDriverEntry(
                toZipOrTarEntryName(name, type),
                type,
                template);
    }
}
