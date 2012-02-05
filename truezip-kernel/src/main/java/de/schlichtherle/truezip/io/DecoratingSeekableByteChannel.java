/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;

/**
 * An abstract decorator for a seekable byte channel.
 * This is optimized for performance and <em>without</em> multithreading
 * support.
 *
 * @since   TrueZIP 7.2
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@CleanupObligation
public abstract class DecoratingSeekableByteChannel
implements SeekableByteChannel {

    /** The nullable decorated seekable byte channel. */
    protected @Nullable SeekableByteChannel delegate;

    /**
     * Constructs a new decorating seekable byte channel.
     *
     * @param delegate the nullable seekable byte channel to decorate.
     */
    @CreatesObligation
    protected DecoratingSeekableByteChannel(
            final @Nullable @WillCloseWhenClosed SeekableByteChannel delegate) {
        this.delegate = delegate;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return delegate.read(dst);
    }
    
    @Override
    public int write(ByteBuffer src) throws IOException {
        return delegate.write(src);
    }

    @Override
    public long position() throws IOException {
        return delegate.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        delegate.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return delegate.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        delegate.truncate(size);
        return this;
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }
    
    @Override
    @DischargesObligation
    public void close() throws IOException {
        delegate.close();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        final String n = getClass().getName();
        final String d = delegate.toString();
        return new StringBuilder(n.length() + "[delegate=".length() + d.length() + 1)
                .append(n)
                .append("[delegate=")
                .append(d)
                .append(']')
                .toString();
    }
}
