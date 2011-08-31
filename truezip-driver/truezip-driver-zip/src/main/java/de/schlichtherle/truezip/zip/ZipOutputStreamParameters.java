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

import java.util.zip.Deflater;

/**
 * An interface for {@link ZipOutputStream} parameters.
 * 
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ZipOutputStreamParameters
extends ZipCharsetParameters {

    /**
     * Returns the initial size (not capacity) of the internal hash map to hold
     * the entries.
     * When appending to an existing archive file, the number of entries in the
     * appendee is added to this property.
     * 
     * @return The initial size (not capacity) of the internal hash map to hold
     * the entries.
     */
    int getInitialSize();

    /**
     * Returns the default compression method for entries.
     * This property is only used if a {@link ZipEntry} does not specify a
     * compression method.
     * Legal values are {@link ZipEntry#STORED}, {@link ZipEntry#DEFLATED}
     * and {@link ZipEntry#BZIP2}.
     *
     * @return The default compression method for entries.
     * @see    ZipEntry#getMethod
     */
    int getMethod();

    /**
     * Returns the compression level for entries.
     * This property is only used if the effective compression method is
     * {@link ZipEntry#DEFLATED} or {@link ZipEntry#BZIP2}.
     * Legal values are {@link Deflater#DEFAULT_COMPRESSION} or range from
     * {@code Deflater#BEST_SPEED} to {@code Deflater#BEST_COMPRESSION}.
     * 
     * @return The compression level for entries.
     */
    int getLevel();
}
