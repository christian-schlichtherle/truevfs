/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.nio.charset.Charset;
import java.util.zip.Deflater;
import net.jcip.annotations.ThreadSafe;

/**
 * Default implementation of {@link ZipOutputStreamParameters}.
 * 
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
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
     * returns {@code ZipOutputStream#OVERHEAD_SIZE}.
     */
    @Override
    public int getOverheadSize() {
        return ZipOutputStream.OVERHEAD_SIZE;
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
