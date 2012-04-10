/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import de.truezip.kernel.util.Maps;
import java.nio.charset.Charset;
import java.util.zip.Deflater;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Default implementation of {@link ZipOutputStreamParameters}.
 * 
 * @author  Christian Schlichtherle
 */
@ThreadSafe
class DefaultZipOutputStreamParameters
extends DefaultZipCharsetParameters
implements ZipOutputStreamParameters {
    
    DefaultZipOutputStreamParameters(Charset charset) {
        super(charset);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link DefaultZipOutputStreamParameters}
     * returns {@code Maps#OVERHEAD_SIZE}.
     */
    @Override
    public int getOverheadSize() {
        return Maps.OVERHEAD_SIZE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link DefaultZipOutputStreamParameters}
     * returns {@code ZipEntry#DEFLATED}.
     */
    @Override
    public int getMethod() {
        return ZipEntry.DEFLATED;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link DefaultZipOutputStreamParameters}
     * returns {@code Deflater#DEFAULT_COMPRESSION}.
     */
    @Override
    public int getLevel() {
        return Deflater.DEFAULT_COMPRESSION;
    }
}