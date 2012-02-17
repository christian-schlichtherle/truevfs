/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.OutputShop;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.annotation.concurrent.Immutable;

/**
 * An archive driver which reads Self Executable (SFX/EXE) ZIP files,
 * but doesn't support to create or update them because this would spoil the
 * SFX code in its preamble.
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public class ReadOnlySfxDriver extends ZipDriver {

    /**
     * The character set used in SFX archives by default, which is determined
     * by calling {@code System.getProperty("file.encoding")}.
     */
    public static final Charset SFX_CHARSET
            = Charset.forName(System.getProperty("file.encoding"));

    public ReadOnlySfxDriver(IOPoolProvider provider) {
        super(provider, SFX_CHARSET);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipDriver}
     * returns {@code true}.
     * 
     * @return {@code true}
     */
    @Override
    public final boolean getPreambled() {
        return true;
    }

    @Override
    protected final OutputShop<ZipDriverEntry> newOutputShop(
            final FsModel model,
            final OptionOutputSocket output,
            final ZipInputShop source)
    throws IOException {
        assert null != model;
        assert null != output;
        throw new FileNotFoundException(
                "driver class does not support creating or modifying SFX archives");
    }
}
