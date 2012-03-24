/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
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
 * @deprecated This class will be removed in TrueZIP 8.
 */
@Deprecated
@NotThreadSafe
final class LazyInputSocket<E extends Entry>
extends DecoratingInputSocket<E> {

    LazyInputSocket(InputSocket<? extends E> input) {
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
    private final class ProxyReadOnlyFile extends DecoratingReadOnlyFile {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        ProxyReadOnlyFile() {
            super(null);
        }

        ReadOnlyFile getDelegate() throws IOException {
            final ReadOnlyFile rof = delegate;
            return null != rof
                    ? rof
                    : (delegate = getBoundDelegate().newReadOnlyFile());
        }

        @Override
        public long length() throws IOException {
            return getDelegate().length();
        }

        @Override
        public long getFilePointer() throws IOException {
            return getDelegate().getFilePointer();
        }

        @Override
        public void seek(long pos) throws IOException {
            getDelegate().seek(pos);
        }

        @Override
        public int read() throws IOException {
            return getDelegate().read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return getDelegate().read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            final ReadOnlyFile rof = delegate;
            if (null != rof)
                rof.close();
        }
    } // ProxyReadOnlyFile

    @NotThreadSafe
    private final class ProxyInputStream extends DecoratingInputStream {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        ProxyInputStream() {
            super(null);
        }

        InputStream getDelegate() throws IOException {
            final InputStream in = delegate;
            return null != in
                    ? in
                    : (delegate = getBoundDelegate().newInputStream());
        }

        @Override
        public int read() throws IOException {
            return getDelegate().read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return getDelegate().read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return getDelegate().skip(n);
        }

        @Override
        public int available() throws IOException {
            return getDelegate().available();
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
                getDelegate().mark(readlimit);
            } catch (IOException ex) {
                throw new IllegalStateException("Could not resolve delegate!", ex);
            }
        }

        @Override
        public void reset() throws IOException {
            getDelegate().reset();
        }

        @Override
        public boolean markSupported() {
            try {
                return getDelegate().markSupported();
            } catch (IOException ignored) {
                return false;
            }
        }
    } // ProxyInputStream
}
