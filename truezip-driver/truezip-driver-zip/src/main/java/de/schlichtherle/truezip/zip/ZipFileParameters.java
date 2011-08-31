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

/**
 * Provides parameters for reading ZIP files.
 * <p>
 * <b>Warning:</b> This interface is <em>not</em> intended for public use
 * - its API may change at will without prior notification!
 * 
 * @param   <E> The type of the created ZIP entries.
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface ZipFileParameters<E extends ZipEntry>
extends ZipCharsetParameters, ZipEntryFactory<E> {

    /**
     * Returns the flag for allowing a preamble.
     * <p>
     * If this method returns {@code true}, then a ZIP file is allowed to
     * contain arbitrary data as its preamble before the actual ZIP file data.
     * Self Extracting Archives typically use a preamble to store the
     * application code that is required to extract the ZIP file contents.
     * <p>
     * If this method returns {@code false}, the a ZIP file must start with
     * either a Local File Header (LFH) signature,
     * a ZIP64 End Of Central Directory Record (EOCDR) signature or an End Of
     * Central Directory Record (EOCDR) signature.
     *
     * @return The flag for allowing a preamble.
     */
    boolean getPreambled();

    /**
     * Returns the flag for allowing a postamble of arbitrary length.
     * <p>
     * If this method returns {@code true}, then a ZIP file is allowed to
     * contain arbitrary data of arbitrary length as its postamble after the
     * actual ZIP file data.
     * Note that searching for an arbitrary length postamble can seriously
     * degrade the performance when reading a false positive ZIP file, i.e.
     * an arbitrary file which not compatible to the ZIP File Format
     * Specification, because then the entire file needs to get searched for
     * an End Of Central Directory Record (EOCDR) signature.
     * So this should only be used if self extracting ZIP files with very large
     * postambles need to get supported.
     * <p>
     * If this method returns {@code false}, then a ZIP file may still have a
     * postamble, but it must not exceed 64KB size, including the End Of
     * Central Directory record with the ZIP file comment.
     * This causes the reading of a false positive ZIP file to fail fast.
     *
     * @return The flag for allowing a postamble of arbitrary length.
     */
    boolean getPostambled();
}
