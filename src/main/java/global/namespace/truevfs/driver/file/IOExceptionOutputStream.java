/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.file;

import global.namespace.truevfs.commons.io.DecoratingOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

/**
 * A decorating output stream which saves the last {@link IOException}
 * in a {@linkplain #exception protected field} for later use.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
abstract class IOExceptionOutputStream extends DecoratingOutputStream {

    /** The nullable last I/O exception. */
    Optional<IOException> exception = Optional.empty();

    /**
     * Constructs a new I/O exception output stream.
     *
     * @param out the nullable output stream to decorate.
     */
    IOExceptionOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(final int b) throws IOException {
        try {
            out.write(b);
        } catch (IOException e) {
            exception = Optional.of(e);
            throw e;
        }
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        try {
            out.write(b, off, len);
        } catch (IOException e) {
            exception = Optional.of(e);
            throw e;
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            out.flush();
        } catch (IOException e) {
            exception = Optional.of(e);
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            out.close();
        } catch (IOException e) {
            exception = Optional.of(e);
            throw e;
        }
    }
}
