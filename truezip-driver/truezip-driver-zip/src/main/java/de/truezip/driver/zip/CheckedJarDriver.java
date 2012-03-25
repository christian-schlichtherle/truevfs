/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.schlichtherle.truezip.cio.IOPoolProvider;
import javax.annotation.concurrent.Immutable;

/**
 * An archive driver for JAR files which checks the CRC-32 value for all ZIP
 * entries in input archives.
 * The additional CRC-32 computation makes this class slower than its super
 * class.
 * <p>
 * If there is a mismatch of the CRC-32 values for a ZIP entry in an input
 * archive, the {@link java.io.InputStream#close} method of the corresponding
 * stream for the archive entry will throw a
 * {@link de.truezip.driver.zip.io.CRC32Exception}.
 * Other than this, the archive entry will be processed normally.
 * So if just the CRC-32 value for the entry in the archive file has been
 * modified, you can still read its entire contents.
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 * 
 * @author  Christian Schlichtherle
 */
@Immutable
public class CheckedJarDriver extends JarDriver {

    public CheckedJarDriver(IOPoolProvider provider) {
        super(provider);
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@code true}
     */
    @Override
    protected boolean check(ZipInputService input, ZipDriverEntry entry) {
        return true;
    }
}