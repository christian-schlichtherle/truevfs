/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipException;

/**
 * A method for writing a ZIP entry.
 * <p>
 * Implementations cannot be thread-safe.
 *
 * @author  Christian Schlichtherle
 */
interface OutputMethod {

    /**
     * Checks the given {@code entry} and updates it.
     * This method may be called multiple times, so it must be idempotent with
     * respect to its side effects on {@code entry}!
     * 
     * @param  entry the ZIP entry to check and update.
     * @throws ZipException if checking the given entry failed for
     *         some reason.
     */
    void init(ZipEntry entry) throws ZipException;

    /**
     * Starts writing the initialized ZIP entry and returns an output stream
     * for writing its contents.
     * You must call {@link #finish()} after writing the contents to the
     * returned output stream.
     * You must not call {@link OutputStream#close()} on the returned output
     * stream!
     * 
     * @return The decorated output stream.
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