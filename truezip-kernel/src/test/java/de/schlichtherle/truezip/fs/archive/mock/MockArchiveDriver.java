/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.mock;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriver;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.socket.spi.ByteArrayIOPoolService;
import de.schlichtherle.truezip.util.BitField;
import java.io.CharConversionException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class MockArchiveDriver
extends FsCharsetArchiveDriver<MockArchiveDriverEntry> {

    private static final Charset charset = Charset.forName("UTF-8");
    
    private final IOPoolProvider provider;
    private final ConcurrentMap<FsMountPoint, MockArchiveDriverEntryContainer>
            containers = new ConcurrentHashMap<FsMountPoint, MockArchiveDriverEntryContainer>();

    public MockArchiveDriver() {
        this(new ByteArrayIOPoolService(0));
    }

    public MockArchiveDriver(final IOPoolProvider provider) {
        super(charset);
        provider.get(); // NPE-check
        this.provider = provider;
    }

    @Override
    protected IOPool<?> getPool() {
        return provider.get();
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
                n = MockArchiveDriverEntryContainer.create(provider);
        final MockArchiveDriverEntryContainer
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
