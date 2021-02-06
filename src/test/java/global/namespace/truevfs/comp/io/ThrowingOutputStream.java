/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.io;

import global.namespace.truevfs.kernel.api.FsTestConfig;
import global.namespace.truevfs.kernel.api.FsThrowManager;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A decorating output stream which supports throwing exceptions according to
 * {@link FsTestConfig}.
 * 
 * @see     ThrowingInputStream
 * @author  Christian Schlichtherle
 */
public final class ThrowingOutputStream extends DecoratingOutputStream {

    private final FsThrowManager control;

    public ThrowingOutputStream(OutputStream out) {
        this(out, null);
    }

    public ThrowingOutputStream(final OutputStream out,
                                final @CheckForNull FsThrowManager control) {
        super(out);
        this.control = null != control
                ? control
                : FsTestConfig.get().getThrowControl();
    }

    private void checkAllExceptions() throws IOException {
        control.check(this, IOException.class);
        checkUndeclaredExceptions();
    }

    private void checkUndeclaredExceptions() {
        control.check(this, RuntimeException.class);
        control.check(this, Error.class);
    }

    @Override
    public void write(int b) throws IOException {
        checkAllExceptions();
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkAllExceptions();
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkAllExceptions();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        checkAllExceptions();
        out.close();
    }
}