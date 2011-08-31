/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.socket.spi.ByteArrayIOPoolService;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class MockArchiveDriver
extends FsCharsetArchiveDriver<MockArchiveEntry> {

    private static final Charset charset = Charset.forName("UTF-8");
    
    private volatile IOPoolProvider provider;

    public MockArchiveDriver() {
        super(charset);
    }

    @Override
    protected IOPool<?> getPool() {
        final IOPoolProvider provider = this.provider;
        return (null != provider ? provider : (this.provider = new ByteArrayIOPoolService(2048))).get();
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
    public MockArchiveEntry newEntry(   String name,
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
