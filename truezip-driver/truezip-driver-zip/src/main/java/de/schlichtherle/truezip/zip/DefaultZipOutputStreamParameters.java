/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * returns {@code ZipOutputStream#INITIAL_SIZE}.
     */
    @Override
    public int getInitialSize() {
        return ZipOutputStream.INITIAL_SIZE;
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
