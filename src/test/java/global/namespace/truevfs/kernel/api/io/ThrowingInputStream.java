/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api.io;

import global.namespace.truevfs.comp.io.DecoratingInputStream;
import global.namespace.truevfs.kernel.api.FsTestConfig;
import global.namespace.truevfs.kernel.api.FsThrowManager;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.InputStream;

/**
 * A decorating input stream which supports throwing exceptions according to
 * {@link FsTestConfig}.
 * 
 * @see     ThrowingOutputStream
 * @author  Christian Schlichtherle
 */
public final class ThrowingInputStream extends DecoratingInputStream {

    private final FsThrowManager control;

    public ThrowingInputStream(InputStream in) {
        this(in, null);
    }

    public ThrowingInputStream( final InputStream in,
                                final @CheckForNull FsThrowManager control) {
        super(in);
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
    public int read() throws IOException {
        checkAllExceptions();
        return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkAllExceptions();
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        checkAllExceptions();
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        checkAllExceptions();
        return in.available();
    }

    @Override
    public void close() throws IOException {
        checkAllExceptions();
        in.close();
    }

    @Override
    public void mark(int readlimit) {
        checkUndeclaredExceptions();
        in.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        checkAllExceptions();
        in.reset();
    }

    @Override
    public boolean markSupported() {
        checkUndeclaredExceptions();
        return in.markSupported();
    }
}