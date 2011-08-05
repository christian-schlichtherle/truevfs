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
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.NotThreadSafe;

/**
 * A method for writing a ZIP entry.
 * <p>
 * Implementations cannot be thread-safe.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
interface OutputMethod {

    /**
     * Checks and updates the given {@code entry} and returns an output stream
     * for writing its contents.
     * You must call {@link #start()}
     * before writing the ZIP entry contents to the returned output stream.
     * Likewise, you must call {@link #finish()}
     * after writing the ZIP contents to the returned output stream.
     * You must not call {@link OutputStream#close()} on the returned output
     * stream!
     * 
     * @param  entry the ZIP entry to check and update.
     * @return the output stream to write the ZIP entry contents to.
     * @throws IOException if initializing the given ZIP entry is not possible
     *         for some reason.
     */
    OutputStream init(ZipEntry entry) throws IOException;

    /**
     * Starts writing the initialized ZIP entry.
     * 
     * @throws IOException on any I/O error.
     */
    void start() throws IOException;

    /**
     * Finishes writing the initialized ZIP entry.
     *
     * @throws IOException on any I/O error.
     */
    void finish() throws IOException;
}
