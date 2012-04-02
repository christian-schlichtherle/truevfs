/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Adapts a {@link SeekableByteChannel} to the {@code InputStream} interface.
 * The stream supports marking.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
@CleanupObligation
public class SeekableByteChannelInputStream extends InputStream {

    private final ByteBuffer single = ByteBuffer.allocate(1);

    /**
     * The underlying {@link SeekableByteChannel}.
     * All methods in this class throw a {@link NullPointerException} if this
     * hasn't been initialized.
     */
    protected @Nullable SeekableByteChannel sbc;

    /**
     * The position of the last mark.
     * Initialized to {@code -1} to indicate that no mark has been set.
     */
    private long mark = -1;

    /**
     * Adapts the given {@code SeekableByteChannel}.
     *
     * @param sbc The underlying {@code SeekableByteChannel}. May be
     *        {@code null}, but must be initialized before any method
     *        of this class can be used.
     */
    @CreatesObligation
    public SeekableByteChannelInputStream(
            final @CheckForNull @WillCloseWhenClosed SeekableByteChannel sbc) {
        this.sbc = sbc;
    }

    @Override
    public int read() throws IOException {
        single.rewind();
        return 1 == sbc.read(single) ? single.get(0) & 0xff : -1;
    }

    @Override
    public final int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return sbc.read(ByteBuffer.wrap(b, off, len));
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0)
            return 0;
        final long fp = sbc.position(); // should fail when closed
        final long len = sbc.size();
        final long rem = len - fp;
        if (n > rem)
            n = (int) rem;
        sbc.position(fp + n);
        return n;
    }

    @Override
    public int available() throws IOException {
        final long rem = sbc.size() - sbc.position();
        return rem > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rem;
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        sbc.close();
    }

    @Override
    public void mark(final int readlimit) {
        try {
            mark = sbc.position();
        } catch (IOException ex) {
            Logger  .getLogger(SeekableByteChannelInputStream.class.getName())
                    .log(Level.WARNING, ex.getLocalizedMessage(), ex);
            mark = -2;
        }
    }

    @Override
    public void reset() throws IOException {
        if (mark < 0)
            throw new IOException(mark == -1
                    ? "no mark set"
                    : "mark()/reset() not supported by underlying file");
        sbc.position(mark);
    }

    @Override
    public boolean markSupported() {
        try {
            sbc.position(sbc.position());
            return true;
        } catch (IOException failure) {
            return false;
        }
    }    
}
