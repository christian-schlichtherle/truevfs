/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.rof;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import net.jcip.annotations.NotThreadSafe;

/**
 * A {@link ReadOnlyFile} implementation using channels to map the underlying
 * file into memory.
 * This class supports files larger than Integer.MAX_VALUE.
 * <p>
 * <b>Warning:</b> The {@link #close} method in this class cannot reliably
 * release the underlying file on the Windows platform and hence this class
 * is not used in other parts of this library.
 * <p>
 * The reason is that the mapped file remains allocated until the garbage
 * collector frees it even if the file channel and/or the
 * {@code RandomAccessFile} has been closed.
 * Subsequent delete/write operations on the file will then fail.
 * For more information, please refer to
 * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154">
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154</a>.
 * Applications may safely use this class if subsequent access to the file
 * is not required during the life time of the application however.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class MemoryMappedReadOnlyFile extends AbstractReadOnlyFile {

    /** The length of the mapped window. */
    private static final int WINDOW_LEN = Integer.MAX_VALUE;

    private FileChannel channel;
    private long windowOff = -1;
    private ByteBuffer window;

    public MemoryMappedReadOnlyFile(File file) throws FileNotFoundException {
        channel = new FileInputStream(file).getChannel();
        try {
            window(0);
        } catch (IOException ex) {
            FileNotFoundException ex2 = new FileNotFoundException(ex.toString());
            ex2.initCause(ex);
            throw ex2;
        }
        assert window != null;
        assert windowOff == 0;
    }

    /**
     * Returns the number of bytes available in the floating window.
     * The window is positioned so that at least one byte is available
     * unless the end of the file has been reached.
     *
     * @return The number of bytes available in the floating window.
     *         If zero, the end of the file has been reached.
     */
    private int available() throws IOException {
        assertOpen();
        if (window.remaining() <= 0)
            window(windowOff + WINDOW_LEN);
        return window.remaining();
    }

    private void window(long newWindowOff) throws IOException {
        if (windowOff == newWindowOff)
            return;

        final long size = channel.size();
        if (newWindowOff > size)
            newWindowOff = size; // don't move past EOF

        window = channel.map(   FileChannel.MapMode.READ_ONLY, newWindowOff,
                                Math.min(size - newWindowOff, WINDOW_LEN));
        assert window != null;
        windowOff = newWindowOff;
    }

    @Override
    public long length() throws IOException {
        assertOpen();
        return channel.size();
    }

    @Override
    public long getFilePointer() throws IOException {
        assertOpen();
        return windowOff + window.position();
    }

    @Override
    public void seek(final long fp) throws IOException {
        assertOpen();

        if (fp < 0)
            throw new IOException("file pointer must not be negative");
        final long length = length();
        if (fp > length)
            throw new IOException("file pointer (" + fp
                    + ") is larger than file length (" + length + ")");

        window(fp / WINDOW_LEN * WINDOW_LEN); // round down
        window.position((int) (fp % WINDOW_LEN));
    }

    @Override
    public int read() throws IOException {
        return available() > 0 ? window.get() & 0xff : -1;
    }

    @Override
    public int read(final byte[] buf, final int off, int len)
    throws IOException {
        if (len == 0)
            return 0; // be fault-tolerant and compatible to RandomAccessFile

        // Check state.
        final int avail = available();
        if (avail <= 0)
            return -1; // EOF

        // Check parameters.
        if (buf == null)
            throw new NullPointerException("buf");
        if (off < 0 || len < 0 || off + len > buf.length)
            throw new IndexOutOfBoundsException();

        if (len > avail)
            len = avail;
        window.get(buf, off, len);
        return len;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DM_GC")
    @Override
    public void close() throws IOException {
        final FileChannel channel = this.channel;
        if (channel == null)
            return;
        this.window = null;
        this.channel = null;
        channel.close();
    }

    /**
     * Ensures that this file is open.
     *
     * @throws IOException If the preconditions do not hold.
     */
    private void assertOpen() throws IOException {
        if (channel == null)
            throw new IOException("file is closed");
    }
}
