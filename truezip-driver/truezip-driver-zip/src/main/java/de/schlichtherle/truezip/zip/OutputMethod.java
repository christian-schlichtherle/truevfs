/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.concurrent.NotThreadSafe;

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
interface OutputMethod {

    /**
     * Checks the given {@code entry} and updates it.
     * This method may be called multiple times, so it must be reentrant!
     * 
     * @param  entry the ZIP entry to check and update.
     * @throws IOException if checking the given entry failed for some reason.
     */
    void init(ZipEntry entry) throws IOException;

    /**
     * Starts writing the initialized ZIP entry and returns an output stream
     * for writing its contents.
     * You must call {@link #finish()} after writing the contents to the
     * returned output stream.
     * You must not call {@link OutputStream#close()} on the returned output
     * stream!
     * 
     * @throws IOException on any I/O error.
     */
    OutputStream start() throws IOException;

    /**
     * Finishes writing the initialized ZIP entry.
     *
     * @throws IOException on any I/O error.
     */
    void finish() throws IOException;
}
