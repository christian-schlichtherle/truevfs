/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.sfx;

import java.io.IOException;
import java.nio.charset.Charset;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;
import net.truevfs.component.zip.driver.AbstractZipDriverEntry;
import net.truevfs.component.zip.driver.ZipDriver;
import net.truevfs.component.zip.driver.ZipDriverEntry;
import net.truevfs.kernel.spec.*;
import net.truevfs.kernel.spec.cio.InputService;
import net.truevfs.kernel.spec.cio.OutputService;
import de.schlichtherle.truecommons.shed.BitField;

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
