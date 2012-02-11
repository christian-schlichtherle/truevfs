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
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriver;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.socket.spi.ByteArrayIOPoolService;
import de.schlichtherle.truezip.util.BitField;
import java.io.CharConversionException;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class MockArchiveDriver
extends FsCharsetArchiveDriver<MockArchiveEntry> {

    private static final Charset charset = Charset.forName("UTF-8");
    
    private final IOPool<?> ioPool;

    public MockArchiveDriver() {
        this(new ByteArrayIOPoolService(32));
    }

    public MockArchiveDriver(final IOPoolProvider provider) {
        super(charset);
        this.ioPool = provider.get();
    }

    @Override
    protected IOPool<?> getPool() {
        return ioPool;
    }

    @Override
    public InputShop<MockArchiveEntry> newInputShop(
            FsModel model,
            InputSocket<?> input)
    throws IOException {
        throw new UnsupportedOperationException("The mock archive driver does not support I/O.");
    }

    @Override
    public OutputShop<MockArchiveEntry> newOutputShop(
            FsModel model,
            OutputSocket<?> output,
            InputShop<MockArchiveEntry> source)
    throws IOException {
        throw new UnsupportedOperationException("The mock archive driver does not support I/O.");
    }

    @Override
    public MockArchiveEntry newEntry(
            String name,
            Type type,
            Entry template,
            BitField<FsOutputOption> mknod)
    throws CharConversionException {
        return new MockArchiveEntry(
                toZipOrTarEntryName(name, type),
                type,
                template);
    }
}
