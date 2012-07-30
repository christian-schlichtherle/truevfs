/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.sfx;

import net.java.truevfs.kernel.spec.FsReadOnlyFileSystemException;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsEntryName;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.component.zip.driver.AbstractZipDriverEntry;
import net.java.truevfs.component.zip.driver.ZipDriver;
import net.java.truevfs.component.zip.driver.ZipDriverEntry;
import net.java.truevfs.kernel.spec.cio.InputService;
import net.java.truevfs.kernel.spec.cio.OutputService;
import net.java.truecommons.shed.BitField;

/**
 * An archive driver which reads Self Executable (SFX/EXE) ZIP files,
 * but doesn't support to create or update them because this would spoil the
 * SFX code in its preamble.
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public class ReadOnlySfxDriver extends ZipDriver {

    /**
     * The character set for entry names and comments, which is the default
     * character set.
     */
    public static final Charset SFX_CHARSET = Charset.defaultCharset();

    /**
     * {@inheritDoc}
     * 
     * @return {@link #SFX_CHARSET}.
     */
    @Override
    public Charset getCharset() {
        return SFX_CHARSET;
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
    public final OutputService<ZipDriverEntry> newOutput(
            FsModel model,
            BitField<FsAccessOption> options,
            FsController<?> controller,
            FsEntryName name,
            @CheckForNull @WillNotClose InputService<ZipDriverEntry> input)
    throws IOException {
        throw new FsReadOnlyFileSystemException();
    }
}
