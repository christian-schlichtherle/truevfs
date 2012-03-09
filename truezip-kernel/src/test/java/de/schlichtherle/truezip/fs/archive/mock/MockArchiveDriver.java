/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.mock;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriver;
import de.schlichtherle.truezip.socket.*;
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
    private final ConcurrentMap<FsMountPoint, MockArchiveDriverEntryContainer>
            containers;

    public MockArchiveDriver() {
        this(null);
    }

    public MockArchiveDriver(@CheckForNull TestConfig config) {
        super(charset);
        if (null == config)
            config = TestConfig.get();
        this.config = config;
        this.containers = new ConcurrentHashMap<FsMountPoint, MockArchiveDriverEntryContainer>(
                Maps.initialCapacity(config.getNumEntries()));
    }

    private IOPoolProvider getIOPoolProvider() {
        return config.getIOPoolProvider();
    }

    @Override
    protected IOPool<?> getPool() {
        return getIOPoolProvider().get();
    }

    @Override
    public InputShop<MockArchiveDriverEntry> newInputShop(
            final FsModel model,
            final InputSocket<?> input)
    throws IOException {
        final FsMountPoint mp = model.getMountPoint();
        input.getLocalTarget(); // don't care for the result
        final MockArchiveDriverEntryContainer
                c = containers.get(mp);
        if (null == c)
            throw new FileNotFoundException(mp.toString());
        return c.newInputShop();
    }

    @Override
    public OutputShop<MockArchiveDriverEntry> newOutputShop(
            FsModel model,
            OutputSocket<?> output,
            InputShop<MockArchiveDriverEntry> source)
    throws IOException {
        final FsMountPoint mp = model.getMountPoint();
        output.getLocalTarget(); // don't care for the result
        final MockArchiveDriverEntryContainer
                n = MockArchiveDriverEntryContainer.create(config);
        MockArchiveDriverEntryContainer o = containers.get(mp);
        if (null == o)
            o = containers.putIfAbsent(mp, n);
        return (null != o ? o : n).newOutputShop();
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
