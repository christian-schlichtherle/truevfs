/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.sfx;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.comp.zipdriver.ZipDriverEntry;
import net.java.truevfs.comp.zipdriver.ZipInputService;

/**
 * An archive driver for SFX/EXE files which checks the CRC32 value for all
 * ZIP entries in input archives.
 * The additional CRC32 computation makes this class slower than its super
 * class.
 * <p>
 * If there is a mismatch of the CRC32 values for a ZIP entry in an input
 * archive, the {@link java.io.InputStream#close} method of the corresponding
 * stream for the archive entry will throw a
 * {@link net.java.truevfs.comp.zip.Crc32Exception}.
 * Other than this, the archive entry will be processed normally.
 * So if just the CRC32 value for the entry in the archive file has been
 * modified, you can still read its entire contents.
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public class CheckedReadOnlySfxDriver extends ReadOnlySfxDriver {

    /**
     * {@inheritDoc}
     * 
     * @return {@code true}
     */
    @Override
    public boolean check(ZipDriverEntry local, ZipInputService<ZipDriverEntry> input) {
        return true;
    }
}
