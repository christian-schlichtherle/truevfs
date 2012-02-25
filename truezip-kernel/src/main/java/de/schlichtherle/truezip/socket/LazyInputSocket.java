/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A lazy input socket provides proxy read only files and input streams which
 * acquire their underlying local target upon the first read access.
 *
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @see    LazyOutputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class LazyInputSocket<E extends Entry>
extends DecoratingInputSocket<E> {

    public LazyInputSocket(InputSocket<? extends E> input) {
        super(input);
    }

    /**
     * Returns a proxy read only file which acquires its underlying read only
     * file upon the first read access.
     *
     * @return A proxy read only file which acquires its underlying read only
     *         file upon the first read access.
     */
    @Override
    public ReadOnlyFile newReadOnlyFile() {
        return new ProxyReadOnlyFile();
    }

    /**
     * Returns a proxy input stream which acquires its underlying input
     * stream upon the first read access.
     *
     * @return A proxy input stream which acquires its underlying input
     *         stream upon the first read access.
     */
    @Override
    public InputStream newInputStream() {
        return new ProxyInputStream();
    }

    @NotThreadSafe
    private class ProxyReadOnlyFile extends DecoratingReadOnlyFile {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        ProxyReadOnlyFile() {
            super(null);
        }

        ReadOnlyFile getReadOnlyFile() throws IOException {
            final ReadOnlyFile rof = delegate;
            return null != rof ? rof : (delegate = getBoundSocket().newReadOnlyFile());
        }

        @Override
        public long length() throws IOException {
            return getReadOnlyFile().length();
        }

        @Override
        public long getFilePointer() throws IOException {
            return getReadOnlyFile().getFilePointer();
        }

        @Override
        public void seek(long pos) throws IOException {
            getReadOnlyFile().seek(pos);
        }

        @Override
        public int read() throws IOException {
            return getReadOnlyFile().read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return getReadOnlyFile().read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            final ReadOnlyFile rof = delegate;
            if (null != rof)
                rof.close();
        }
    } // ProxyReadOnlyFile

    @NotThreadSafe
    private class ProxyInputStream extends DecoratingInputStream {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        ProxyInputStream() {
            super(null);
        }

        InputStream getInputStream() throws IOException {
            final InputStream in = delegate;
            return null != in ? in : (delegate = getBoundSocket().newInputStream());
        }

        @Override
        public int read() throws IOException {
            return getInputStream().read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return getInputStream().read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return getInputStream().skip(n);
        }

        @Override
        public int available() throws IOException {
            return getInputStream().available();
        }

        @Override
        public void close() throws IOException {
            final InputStream in = delegate;
            if (null != in)
                in.close();
        }

        @Override
        public void mark(int readlimit) {
            try {
                getInputStream().mark(readlimit);
            } catch (IOException ex) {
                // The caller should have called markSupported() before
                // this method. If the underlying input stream isn't
                // available, an IOException should have been thrown there.
                // So most likely the caller did not call markSupported()
                // before, which is a violation of the interface contract.
                throw new AssertionError(ex);
            }
        }

        @Override
        public void reset() throws IOException {
            getInputStream().reset();
        }

        @Override
        public boolean markSupported() {
            try {
                return getInputStream().markSupported();
            } catch (IOException ignored) {
                return false;
            }
        }
    } // ProxyInputStream
}
