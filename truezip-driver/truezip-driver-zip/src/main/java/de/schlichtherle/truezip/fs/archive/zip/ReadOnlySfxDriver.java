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
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import net.jcip.annotations.Immutable;

/**
 * An archive driver which reads Self Executable (SFX/EXE) ZIP files,
 * but doesn't support to create or update them because this would spoil the
 * SFX code in its preamble.
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

    public ReadOnlySfxDriver(IOPoolProvider ioPoolProvider) {
        super(ioPoolProvider, SFX_CHARSET);
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
    protected final OutputShop<ZipArchiveEntry> newOutputShop(
            final FsModel model,
            final OptionOutputSocket output,
            final @CheckForNull ZipInputShop source)
    throws IOException {
        throw new FileNotFoundException(
                "driver class does not support creating or modifying SFX archives");
    }
}
