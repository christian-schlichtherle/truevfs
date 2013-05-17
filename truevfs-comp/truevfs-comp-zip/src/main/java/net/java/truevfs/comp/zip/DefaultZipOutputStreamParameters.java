/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import net.java.truecommons.shed.HashMaps;
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
        return HashMaps.OVERHEAD_SIZE;
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